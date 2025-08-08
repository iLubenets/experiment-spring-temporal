package io.github.ilubenets.springtemporal.application;

import io.github.ilubenets.springtemporal.domain.Document;
import io.github.ilubenets.springtemporal.domain.ResolveIncidentSignal;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

import static io.github.ilubenets.springtemporal.application.SearchAttributeKeys.STATE;
import static io.github.ilubenets.springtemporal.application.SearchAttributeKeys.State;

@WorkflowImpl(taskQueues = CreateInvoiceWorkflowImpl.TASK_QUEUE)
public final class CreateInvoiceWorkflowImpl
    implements CreateInvoiceWorkflow {

    private static final Logger LOG = Workflow.getLogger(CreateInvoiceWorkflowImpl.class);

    public static final String TASK_QUEUE = "CreateRetailInvoiceWorkflowQueue";

    // inline definition for activity stub
    private final DocumentActivities documentActivities = Workflow.newActivityStub(
        DocumentActivities.class,
        ActivityOptions.newBuilder()
            .setTaskQueue(DocumentActivitiesImpl.TASK_QUEUE) // reference to specific act impl by queue
            .setStartToCloseTimeout(Duration.ofSeconds(60))
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setMaximumAttempts(2)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .build()
            )
            .build()
    );
    // newActivityStub defined in impl
    private final AccountingActivities accountingActivities = AccountingActivitiesImpl.newActivityStub();
    private final ExperimentActivities experimentActivities = ExperimentActivitiesImpl.newActivityStub();

    // WF is stateful
    private ResolveIncidentSignal resolveIncidentSignal = null;
    private Document document = null;

    // Workflow logic requirements: https://docs.temporal.io/develop/java/core-application#workflow-logic-requirements
    // Core: https://docs.temporal.io/develop/java/core-application
    @Override
    public void create(final String processId) {
        Workflow.upsertTypedSearchAttributes(STATE.valueSet(State.RUNNING.name()));
        LOG.info("WF: started [{}]", processId);

        document = documentActivities.requireDocument(processId);
        LOG.info("WF: document [{}]", document);

        var total = experimentActivities.calculateTotalOn2ndTry(document.charges());
        LOG.info("WF: total calculated [{}]", total);

        document = document.setTotal(total);
        documentActivities.updateDocument(document);
        LOG.info("WF: total saved");

        var documentId = accountingActivities.createDocumentId(processId);
        LOG.info("WF: document id generated [{}]", documentId);

        // incident handling examples
        switch (document.example()) {
            case INCIDENT_CHILD_WF:
                exampleChildWFIncident(processId, documentId, true);
                break;
            case INCIDENT_EMBEDDED:
            default:
                exampleEmbeddedIncident(processId, documentId);
        }

        // Versioning

        // best practice: keep version definition even after migration
        Workflow.getVersion("OldDecommissionedMigration", 0, 1);
        // here was some activity which was changed/removed in version #1

        // ongoing migration
        var version = Workflow.getVersion("ChangeSucceedOnlyAfter3Attempts", 0, 2);
        if (version == 0) {
            // old version
            experimentActivities.succeedOnlyAfter3Attempts();
        }  else if (version == 1) {
            // mid version
            // some logic
        } else {
            // latest version
            experimentActivities.succeedOnlyAfter2Attempt();
        }

        Workflow.upsertTypedSearchAttributes(STATE.valueSet(State.COMPLETED.name()));
        LOG.info("WF: done [{}]", this.document);
    }

    private void exampleChildWFIncident(final String processId, final String documentId, final boolean isFailBookDocument) {
        var bookDocumentWF = Workflow.newChildWorkflowStub(
            BookDocumentChildWorkflow.class,
            ChildWorkflowOptions.newBuilder()
                .setTaskQueue(BookDocumentChildWorkflowImpl.TASK_QUEUE)
                .setWorkflowId(processId + "-" + documentId)
                .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY)
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setMaximumAttempts(2)
                        .setInitialInterval(Duration.ofSeconds(1))
                        .build()
                )
                .build()
        );

        try {
            // also can be done asynchronously:
            // Promise<Document> documentPromise = Async.function(bookDocumentWF::book, processId, documentId, isFailBookDocument);
            document = bookDocumentWF.book(processId, documentId, isFailBookDocument);
        } catch (final ChildWorkflowFailure e) {
            LOG.warn("WF: accounting failed with incident [{}] ", e.getMessage(), e);
            Workflow.upsertTypedSearchAttributes(STATE.valueSet(State.INCIDENT.name()));
            Workflow.await(() -> resolveIncidentSignal != null);
            Workflow.upsertTypedSearchAttributes(STATE.valueSet(State.RUNNING.name()));

            resolveIncidentSignal = null;

            // recursion
            exampleChildWFIncident(processId, documentId, false);
        }
    }

    /**
     * Infinit loop with incident management via Signal
     */
    private void exampleEmbeddedIncident(final String processId, final String documentId) {
        var retryDelay = Duration.ofSeconds(2);
        var totalAttempts = 0;
        var maxAttempts = 2;
        var isFailBookDocument = true;
        while (true) {
            totalAttempts++;
            var number = documentActivities.generateDocumentNumber(processId);
            try {
                // save document to DB internally and return updated version
                document = accountingActivities.bookDocument(processId, documentId, number, isFailBookDocument);
                documentActivities.confirmDocumentNumber(processId, number);
                return;
            } catch (final ActivityFailure e) {
                LOG.warn("WF: accounting failed [{}] activity [{}]", e.getMessage(), e.getActivityType(), e);
                documentActivities.cancelDocumentNumber(number);

                // resolve incident with SIGNAL
                if (totalAttempts < maxAttempts) {
                    Workflow.sleep(retryDelay);
                } else {
                    LOG.error("WF: failed waiting for human action - [{}]", e.getMessage(), e);
                    // we can use customer search field to track custom states
                    Workflow.upsertTypedSearchAttributes(STATE.valueSet(State.INCIDENT.name()));
                    Workflow.await(() -> resolveIncidentSignal != null);
                    Workflow.upsertTypedSearchAttributes(STATE.valueSet(State.RUNNING.name()));
                    //
                    // Some actions to resolve incident
                    //
                    isFailBookDocument = false;
                    maxAttempts += 1;
                    resolveIncidentSignal = null;
                }
            }
        }
    }

    @Override
    public void resolveIncidentSignal(final ResolveIncidentSignal signal) {
        LOG.info("WF: Received ResolveIncidentSignal [{}]", signal);
        this.resolveIncidentSignal = signal;
    }

    @Override
    public Document getDocument() {
        return document;
    }

    @Override
    public void updateDocument(final Document document) {
        this.document = document;
        documentActivities.updateDocument(document);
    }
}
