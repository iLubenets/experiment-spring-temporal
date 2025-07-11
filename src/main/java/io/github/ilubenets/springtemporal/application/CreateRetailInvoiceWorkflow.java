package io.github.ilubenets.springtemporal.application;

import io.github.ilubenets.springtemporal.domain.Document;
import io.github.ilubenets.springtemporal.domain.ResolveIncidentSignal;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface CreateRetailInvoiceWorkflow {

    @WorkflowMethod
    void create(Document document);

    @SignalMethod(name = "ResolveIncidentSignal")
    void resolveIncidentSignal(ResolveIncidentSignal signal);
}
