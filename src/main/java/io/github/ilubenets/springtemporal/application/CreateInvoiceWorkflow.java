package io.github.ilubenets.springtemporal.application;

import io.github.ilubenets.springtemporal.domain.Document;
import io.github.ilubenets.springtemporal.domain.ResolveIncidentSignal;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CreateInvoiceWorkflow {

    @WorkflowMethod
    void create(String processId);

    @SignalMethod(name = "ResolveIncidentSignal")
    void resolveIncidentSignal(ResolveIncidentSignal signal);

    @QueryMethod
    Document getDocument();

    @UpdateMethod
    void updateDocument(Document document);
}
