package io.github.ilubenets.springtemporal.application;

import java.time.Duration;
import java.util.Map;

import io.github.ilubenets.springtemporal.domain.Document;
import io.github.ilubenets.springtemporal.domain.ResolveIncidentSignal;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

@WorkflowImpl(taskQueues = CreateRetailInvoiceWorkflowImpl.TASK_QUEUE)
public final class CreateRetailInvoiceWorkflowImpl
    implements CreateRetailInvoiceWorkflow {

    private static final Logger LOG = Workflow.getLogger(CreateRetailInvoiceWorkflowImpl.class);

    public static final String TASK_QUEUE = "CreateRetailInvoiceWorkflowQueue";

    private final DocumentNumberActivities documentActivities = Workflow.newActivityStub(
        DocumentNumberActivities.class,
        ActivityOptions.newBuilder()
            .setTaskQueue(DocumentNumberActivitiesImpl.TASK_QUEUE) // reference to specific act impl by queue
            .setStartToCloseTimeout(Duration.ofSeconds(60))
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setMaximumAttempts(2)
                    .setInitialInterval(Duration.ofSeconds(1))
                    .build()
            )
            .build()
    );
    private final AccountingActivities accountingActivities = Workflow.newActivityStub(
        AccountingActivities.class,
        ActivityOptions.newBuilder()
            .setTaskQueue(AccountingActivitiesImpl.TASK_QUEUE)
            .setStartToCloseTimeout(Duration.ofSeconds(60))
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setMaximumAttempts(2)
                    .setInitialInterval(Duration.ofSeconds(10))
                    .build()
            )
            .build()
    );
    private final ExperimentActivities experimentActivities = Workflow.newActivityStub(
        ExperimentActivities.class,
        ActivityOptions.newBuilder()
            .setTaskQueue(ExperimentActivitiesImpl.TASK_QUEUE)
            .setStartToCloseTimeout(Duration.ofMinutes(1))
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setMaximumAttempts(5)
                    .setBackoffCoefficient(2.0)
                    .setInitialInterval(Duration.ofSeconds(5))
                    .setMaximumInterval(Duration.ofMinutes(1))
                    .build()
            )
            .build()
    );

    private ResolveIncidentSignal resolveIncidentSignal = null;

    public CreateRetailInvoiceWorkflowImpl() {
    }

    // Workflow logic requirements: https://docs.temporal.io/develop/java/core-application#workflow-logic-requirements
    // Core: https://docs.temporal.io/develop/java/core-application
    @Override
    public void create(final Document document) {
        var processId = document.processId();
        LOG.info("WF: started {}", processId);

        var total = accountingActivities.calculateTotal(processId);
        LOG.info("WF: total calculated {}", total);

        var documentId = accountingActivities.createDocumentId(processId);

        if (document.productName().equals("FATAL")) {
            throw ApplicationFailure.newNonRetryableFailure("FATAL error", "FATAL_ERROR");
        }

        var number = generateNumberAndBookDocumentWithRetries(document, documentId);
        documentActivities.confirmDocumentNumber(processId, number);
        Workflow.upsertMemo(Map.of("documentNumber", number));

        var version = Workflow.getVersion("ChangeSucceedOnlyAfter3Attempts", 0, 1);
        if (version == 0) {
            experimentActivities.succeedOnlyAfter3Attempts();
        } else {
            experimentActivities.succeedOnlyAfter2Attempt();
        }

        LOG.info("WF: done {}", document);
    }

    private String generateNumberAndBookDocumentWithRetries(final Document document, final String documentId) {
        var isFailBookDocument = document.config().activitiesToFail().contains("bookDocument");
        var retryDelay = Duration.ofSeconds(3);
        var totalAttempts = 0;
        var maxAttempts = 3;
        while (true) {
            LOG.info("WF: info {}", Workflow.getInfo());

            totalAttempts++;
            var number = documentActivities.generateDocumentNumber(document.processId());
            LOG.info("WF: got number & id {}:{}", number, documentId);
            try {
                accountingActivities.bookDocument(document.processId(), documentId, number, isFailBookDocument);
                LOG.info("WF: document booked {}:{}", number, documentId);
                return number;
            } catch (ActivityFailure e) {
                LOG.error("WF: accounting fail {}", e.getMessage());

                documentActivities.cancelDocumentNumber(number);
                LOG.info("WF: cancelDocumentNumber {}", number);

                // resolve incident
                if (totalAttempts < maxAttempts) {
                    Workflow.sleep(retryDelay);
                } else {
                    Workflow.await(Duration.ofDays(300), () -> resolveIncidentSignal != null);
                    isFailBookDocument = false;
                    maxAttempts += 1;
                    resolveIncidentSignal = null;
                }
            }
        }
    }

    @Override
    public void resolveIncidentSignal(final ResolveIncidentSignal signal) {
        LOG.info("WF: Received ResolveIncidentSignal {}", signal);
        this.resolveIncidentSignal = signal;
    }
}
