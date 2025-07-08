package io.github.ilubenets.springtemporal.application;

import io.temporal.activity.Activity;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = ExperimentActivitiesImpl.TASK_QUEUE)
public final class ExperimentActivitiesImpl
    implements ExperimentActivities {

    public static final String TASK_QUEUE = "ExperimentActivitiesQueue";

    @Override
    public void succeedOnlyAfter3Attempts() {
        var ctx = Activity.getExecutionContext();
        var attempt = ctx.getInfo().getAttempt();
        if (attempt <= 3) {
            throw new IllegalStateException("Attempt " + attempt + " is less than 3");
        }
    }

    @Override
    public void succeedOnlyAfter1Attempt() {
        var ctx = Activity.getExecutionContext();
        var attempt = ctx.getInfo().getAttempt();
        if (attempt <= 1) {
            throw new IllegalStateException("Attempt " + attempt + " is less than 3");
        }
    }
}
