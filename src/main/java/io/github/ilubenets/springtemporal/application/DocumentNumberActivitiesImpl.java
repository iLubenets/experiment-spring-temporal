package io.github.ilubenets.springtemporal.application;

import io.github.ilubenets.springtemporal.adapter.client.ClientException;
import io.github.ilubenets.springtemporal.adapter.client.DocumentNumberClient;
import io.github.ilubenets.springtemporal.adapter.repository.DocumentPostgresRepository;
import io.github.ilubenets.springtemporal.domain.Document;
import io.temporal.activity.Activity;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
// TODO: important to read
// queue can be unique or the same as WF
// worker automatically created only when "taskQueues" defined
@ActivityImpl(taskQueues = DocumentNumberActivitiesImpl.TASK_QUEUE)
public class DocumentNumberActivitiesImpl
    implements DocumentNumberActivities {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentNumberActivitiesImpl.class);

    public static final String TASK_QUEUE = "DocumentNumberActivitiesQueue";

    private final DocumentNumberClient documentNumberClient;
    private final DocumentPostgresRepository documentPostgresRepository;

    public DocumentNumberActivitiesImpl(
        final DocumentNumberClient documentNumberClient,
        final DocumentPostgresRepository documentPostgresRepository
    ) {
        this.documentNumberClient = documentNumberClient;
        this.documentPostgresRepository = documentPostgresRepository;
    }

    @Override
    public String generateDocumentNumber(final String processId) {
        var document = documentPostgresRepository.require(processId);
        if (document.productName().equals("FATAL")) {
            throw ApplicationFailure.newNonRetryableFailure("FATAL error", "FATAL_ERROR");
        }
        try {
            return documentNumberClient.generate(document.productName(), document.config());
        } catch (ClientException e) {
            throw Activity.wrap(e);
        }
    }

    @Override
    public void confirmDocumentNumber(final String processId, final String documentNumber) {
        try {
            var document = documentPostgresRepository.require(processId);
            documentNumberClient.confirm(documentNumber);
            documentPostgresRepository.update(document.setDocumentNumber(documentNumber));
        } catch (ClientException e) {
            throw Activity.wrap(e);
        }
    }

    @Override
    public void cancelDocumentNumber(final String documentNumber) {
        try {
            documentNumberClient.cancel(documentNumber);
        } catch (ClientException e) {
            throw Activity.wrap(e);
        }
    }

    @Override
    public Document getDocument(final String processId) {
        return documentPostgresRepository.require(processId);
    }

    @Override
    public void updateDocument(final Document document) {
        documentPostgresRepository.update(document);
    }
}
