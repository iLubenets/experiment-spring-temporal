package io.github.ilubenets.springtemporal.application;

import io.github.ilubenets.springtemporal.adapter.repository.DocumentPostgresRepository;
import io.github.ilubenets.springtemporal.domain.Document;
import io.temporal.activity.Activity;
import io.temporal.spring.boot.ActivityImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Component
// TODO: important to read
// queue can be unique or the same as WF
// worker automatically created only when "taskQueues" defined
@ActivityImpl(taskQueues = DocumentActivitiesImpl.TASK_QUEUE)
public class DocumentActivitiesImpl
    implements DocumentActivities {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentActivitiesImpl.class);

    public static final String TASK_QUEUE = "DocumentNumberActivitiesQueue";

    private final DocumentPostgresRepository documentPostgresRepository;

    public DocumentActivitiesImpl(final DocumentPostgresRepository documentPostgresRepository) {
        this.documentPostgresRepository = documentPostgresRepository;
    }

    @Override
    public String generateDocumentNumber(final String processId) {
        externalCall();
        var number = RandomStringUtils.secure().nextNumeric(6);
        LOG.info("Number generated [{}] for [{}]", number, processId);
        return number;
    }

    @Override
    @Transactional
    public void confirmDocumentNumber(final String processId, final String documentNumber) {
        var document = documentPostgresRepository.require(processId);
        externalCall();
        documentPostgresRepository.update(document.setDocumentNumber(documentNumber));
    }

    @Override
    public void cancelDocumentNumber(final String documentNumber) {
        externalCall();
    }

    @Override
    public Document requireDocument(final String processId) {
        return documentPostgresRepository.require(processId);
    }

    @Override
    public void updateDocument(final Document document) {
        documentPostgresRepository.update(document);
    }

    private static void externalCall() {
        try {
            Thread.sleep(Duration.ofSeconds(2));
        } catch (InterruptedException e) {
            throw Activity.wrap(e);
        }
    }
}
