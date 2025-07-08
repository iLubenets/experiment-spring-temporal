package io.github.ilubenets.springtemporal.application;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface DocumentNumberActivities {

    @ActivityMethod
    String generateDocumentNumber(String processId);

    @ActivityMethod
    void confirmDocumentNumber(String processId, String documentNumber);

    @ActivityMethod
    void cancelDocumentNumber(String documentNumber);
}
