package io.github.ilubenets.springtemporal.application;

import io.github.ilubenets.springtemporal.domain.Document;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.ActivityImpl;
import io.temporal.workflow.Workflow;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ActivityImpl(taskQueues = ExperimentActivitiesImpl.TASK_QUEUE)
public final class ExperimentActivitiesImpl
    implements ExperimentActivities {

    public static final String TASK_QUEUE = "ExperimentActivitiesQueue";

    public static ExperimentActivities newActivityStub() {
        return Workflow.newActivityStub(
            ExperimentActivities.class,
            ActivityOptions.newBuilder()
                .setTaskQueue(ExperimentActivitiesImpl.TASK_QUEUE)
                .setStartToCloseTimeout(Duration.ofMinutes(1))
                .setRetryOptions(
                    // https://docs.temporal.io/develop/activity-retry-simulator
                    RetryOptions.newBuilder()
                        .setMaximumAttempts(5)
                        .setBackoffCoefficient(2.0)
                        .setInitialInterval(Duration.ofSeconds(5))
                        .setMaximumInterval(Duration.ofMinutes(1))
                        .build()
                )
                .build()
        );
    }

    @Override
    public void succeedOnlyAfter3Attempts() {
        var ctx = Activity.getExecutionContext();
        var attempt = ctx.getInfo().getAttempt();
        if (attempt <= 3) {
            throw new IllegalStateException("Attempt " + attempt + " is less than 3");
        }
    }

    @Override
    public void succeedOnlyAfter2Attempt() {
        var ctx = Activity.getExecutionContext();
        var attempt = ctx.getInfo().getAttempt();
        if (attempt <= 2) {
            throw new IllegalStateException("Attempt " + attempt + " is less than 3");
        }
    }

    @Override
    public Document.Total calculateTotalOn2ndTry(final List<Document.Charge> charges) {
        var ctx = Activity.getExecutionContext();
        var total = new Document.Total(
            charges.stream().mapToLong(Document.Charge::amount).sum(),
            charges.getFirst().currency()
        );
        if (ctx.getInfo().getAttempt() < 2) {
            throw new RuntimeException("ACT: 1st attempt always fails");
        }
        return total;
    }
}
