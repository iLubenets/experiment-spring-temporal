package io.github.ilubenets.springtemporal.application;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ExperimentActivities {

    @ActivityMethod
    void succeedOnlyAfter3Attempts();

    @ActivityMethod
    void succeedOnlyAfter1Attempt();
}
