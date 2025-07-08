package io.github.ilubenets.springtemporal.application;

import io.github.ilubenets.springtemporal.domain.Document;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface AccountingActivities {

    @ActivityMethod
    String createDocumentId(String processId);

    @ActivityMethod
    void bookDocument(String processId, String documentId, String documentNumber, boolean isFail);

    @ActivityMethod
    Document.Total calculateTotal(String processId);
}
