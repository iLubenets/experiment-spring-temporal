package io.github.ilubenets.springtemporal.application;

import java.util.UUID;

import io.github.ilubenets.springtemporal.adapter.repository.DocumentPostgresRepository;
import io.github.ilubenets.springtemporal.domain.Document;
import io.temporal.activity.Activity;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public String createDocumentId(final String processId) {
        return "sap" + System.currentTimeMillis();
    }

    @Override
    public void bookDocument(final String processId, final String documentId, final String documentNumber, final boolean isFail) {
        LOG.info("ACT: accountingBookDocument {} : {}", documentId, documentNumber);
        var document = documentPostgresRepository.require(processId);
        if (isFail) {
            var failReason = new RuntimeException("Document booking failed");
            throw Activity.wrap(failReason);
        }
        documentPostgresRepository.update(document.setAccountingId(UUID.randomUUID().toString()));
    }

    // TODO: important to read
    // temporal does not handle db transactions
    @Override
    @Transactional
    public Document.Total calculateTotal(final String processId) {
        // TODO: important to read
        // we can access reach context
        var ctx = Activity.getExecutionContext();
        var document = documentPostgresRepository.require(processId);
        LOG.info("ACT: calculateTotal {} current total {}", processId, document.total());

        var total = new Document.Total(
            document.charges().stream().mapToLong(Document.Charge::amount).sum(),
            document.charges().getFirst().currency()
        );
        documentPostgresRepository.update(document.setTotal(total));
        if (ctx.getInfo().getAttempt() <= 1) {
            throw new RuntimeException("ACT: 1st attempt always fails");
        }
        return total;
    }
}
