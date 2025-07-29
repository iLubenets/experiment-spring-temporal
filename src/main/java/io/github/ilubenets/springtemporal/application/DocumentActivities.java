package io.github.ilubenets.springtemporal.application;

import io.github.ilubenets.springtemporal.domain.Document;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface DocumentActivities {

    @ActivityMethod
    String generateDocumentNumber(String processId);

    @ActivityMethod
    void confirmDocumentNumber(String processId, String documentNumber);

    @ActivityMethod
    void cancelDocumentNumber(String documentNumber);

    @ActivityMethod
    Document requireDocument(String processId);

    @ActivityMethod
    void updateDocument(Document document);
}
