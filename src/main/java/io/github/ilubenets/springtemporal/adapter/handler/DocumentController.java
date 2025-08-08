package io.github.ilubenets.springtemporal.adapter.handler;

import io.github.ilubenets.springtemporal.adapter.repository.DocumentPostgresRepository;
import io.github.ilubenets.springtemporal.application.CreateInvoiceWorkflow;
import io.github.ilubenets.springtemporal.application.CreateInvoiceWorkflowImpl;
import io.github.ilubenets.springtemporal.application.SearchAttributeKeys;
import io.github.ilubenets.springtemporal.domain.Document;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.SearchAttributes;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

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
        final var processId = RandomStringUtils.secure().nextAlphabetic(10);
        final var document = new Document(
            processId,
            request.orderId(),
            request.example(),
            request.charges().stream().map(c -> new Document.Charge(c.code(), c.amount(), c.currency())).toList(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        documentRepository.add(document);
        LOG.info("Schedule created document wf: [{}]", document);

        // async
        var workflow = client.newWorkflowStub(
            CreateInvoiceWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(CreateInvoiceWorkflowImpl.TASK_QUEUE) // reference to specific wf impl
                .setWorkflowId(processId)
                .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE_FAILED_ONLY)
                .setTypedSearchAttributes(
                    SearchAttributes.newBuilder()
                        .set(SearchAttributeKeys.STATE, SearchAttributeKeys.State.NEW.name())
                        .set(SearchAttributeKeys.ORDER_ID, document.orderId())
                        .build()
                )
                .build()
        );
        WorkflowClient.start(workflow::create, processId);

        return ResponseEntity.ok(
            new DocumentResponse(processId)
        );
    }

}
