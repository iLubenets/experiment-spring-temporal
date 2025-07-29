package io.github.ilubenets.springtemporal.application;

import io.github.ilubenets.springtemporal.domain.Document;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;

@ActivityInterface
public interface ExperimentActivities {

    @ActivityMethod
    void succeedOnlyAfter3Attempts();

    @ActivityMethod
    void succeedOnlyAfter2Attempt();

    @ActivityMethod
    Document.Total calculateTotalOn2ndTry(List<Document.Charge> charges);
}
