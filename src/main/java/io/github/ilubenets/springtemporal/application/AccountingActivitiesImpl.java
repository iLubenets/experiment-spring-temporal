package io.github.ilubenets.springtemporal.application;

import io.github.ilubenets.springtemporal.adapter.repository.DocumentPostgresRepository;
import io.github.ilubenets.springtemporal.domain.Document;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.ActivityImpl;
import io.temporal.workflow.Workflow;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Component
@ActivityImpl(taskQueues = AccountingActivitiesImpl.TASK_QUEUE)
public class AccountingActivitiesImpl
    implements AccountingActivities {

    private static final Logger LOG = LoggerFactory.getLogger(AccountingActivitiesImpl.class);

    public static final String TASK_QUEUE = "AccountingActivitiesQueue";

    private final DocumentPostgresRepository documentPostgresRepository;

    public AccountingActivitiesImpl(final DocumentPostgresRepository documentPostgresRepository) {
        this.documentPostgresRepository = documentPostgresRepository;
    }

    public static AccountingActivities newActivityStub() {
        return Workflow.newActivityStub(
            AccountingActivities.class,
            ActivityOptions.newBuilder()
                .setTaskQueue(AccountingActivitiesImpl.TASK_QUEUE)
                .setStartToCloseTimeout(Duration.ofSeconds(60))
                .setRetryOptions(
                    RetryOptions.newBuilder()
                        .setMaximumAttempts(2)
                        .setInitialInterval(Duration.ofSeconds(10))
                        .build()
                )
                .build()
        );
    }

    @Override
    public String createDocumentId(final String processId) {
        return "sap-" + RandomStringUtils.secure().nextNumeric(5);
    }

    @Transactional
    @Override
    public Document bookDocument(final String processId, final String documentId, final String documentNumber, final boolean isFail) {
        LOG.info("ACT: accountingBookDocument {} : {}", documentId, documentNumber);
        var document = documentPostgresRepository.require(processId);
        if (isFail) {
            var failReason = new RuntimeException("Document booking failed");
            throw Activity.wrap(failReason);
        }
        documentPostgresRepository.update(document.setAccountingId(UUID.randomUUID().toString()));

        return document;
    }
}
