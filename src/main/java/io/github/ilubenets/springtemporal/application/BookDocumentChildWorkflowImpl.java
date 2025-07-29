package io.github.ilubenets.springtemporal.application;

import io.github.ilubenets.springtemporal.domain.Document;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

@WorkflowImpl(taskQueues = BookDocumentChildWorkflowImpl.TASK_QUEUE)
public final class BookDocumentChildWorkflowImpl
    implements BookDocumentChildWorkflow {

    private static final Logger LOG = Workflow.getLogger(BookDocumentChildWorkflowImpl.class);

    public static final String TASK_QUEUE = "BookDocumentChildWorkflowQueue";

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

    @Override
    public Document book(final String processId, final String documentId, final boolean isFail) {
        var saga = new Saga(new Saga.Options.Builder().build());

        var number = documentActivities.generateDocumentNumber(processId);
        saga.addCompensation(documentActivities::cancelDocumentNumber, number);

        try {
            // save document to DB internally and return updated version
            var document = accountingActivities.bookDocument(processId, documentId, number, isFail);
            documentActivities.confirmDocumentNumber(processId, number);
            return document;
        } catch (final ActivityFailure e) {
            LOG.warn("WF: accounting failed [{}] activity [{}]", e.getMessage(), e.getActivityType(), e);
            saga.compensate();
            throw e;
        }
    }
}
