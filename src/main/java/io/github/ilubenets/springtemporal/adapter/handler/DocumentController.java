package io.github.ilubenets.springtemporal.adapter.handler;

import java.util.Optional;

import io.github.ilubenets.springtemporal.adapter.repository.DocumentPostgresRepository;
import io.github.ilubenets.springtemporal.application.CreateRetailInvoiceWorkflow;
import io.github.ilubenets.springtemporal.application.CreateRetailInvoiceWorkflowImpl;
import io.github.ilubenets.springtemporal.domain.Document;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
)
public final class DocumentController {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentController.class);

    private final WorkflowClient client;
    private final DocumentPostgresRepository documentRepository;

    public DocumentController(final WorkflowClient client, final DocumentPostgresRepository documentRepository) {
        this.client = client;
        this.documentRepository = documentRepository;
    }

    @PostMapping("api/document")
    public ResponseEntity<DocumentResponse> create(@RequestBody final DocumentRequest request) {
        final var processId = "order-" + request.orderId();
        final var document = new Document(
            processId,
            request.productName(),
            request.charges().stream().map(c -> new Document.Charge(c.code(), c.amount(), c.currency())).toList(),
            new Document.ProcessConfig(
                request.config().activitiesToFail(),
                request.config().numberCreationSeconds()
            ),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        documentRepository.add(document);

        LOG.info("Created document: {}", document);

        // async
        var workflow = client.newWorkflowStub(
            CreateRetailInvoiceWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(CreateRetailInvoiceWorkflowImpl.TASK_QUEUE) // reference to specific wf impl
                .setWorkflowId(processId)
                .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY)
                .setTypedSearchAttributes(
                    SearchAttributes.newBuilder()
                        .set(SearchAttributeKey.forKeyword("State"), CreateRetailInvoiceWorkflowImpl.State.OK.name())
                        .set(SearchAttributeKey.forKeyword("ProductName"), document.productName())
                        .set(SearchAttributeKey.forText("OrderId"), document.processId())
                        .build()
                )
                .build()
        );
        WorkflowClient.start(workflow::create, document);

        return ResponseEntity.ok(new DocumentResponse(processId));
    }

}
