package io.github.ilubenets.springtemporal.application;

import io.github.ilubenets.springtemporal.domain.Document;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface BookDocumentChildWorkflow {

    @WorkflowMethod
    Document book(String processId, String documentId, boolean isFail);
}
