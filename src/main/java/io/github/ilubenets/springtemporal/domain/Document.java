package io.github.ilubenets.springtemporal.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record Document(
    String processId,
    String productName,
    List<Charge> charges,
    ProcessConfig config,
    Optional<Total> total,
    Optional<String> documentNumber,
    Optional<String> accountingId
) {
    public Document setDocumentNumber(final String number) {
        return new Document(processId, productName, charges, config, total, Optional.of(number), accountingId);
    }

    public Document setAccountingId(final String id) {
        return new Document(processId, productName, charges, config, total, documentNumber, Optional.of(id));
    }

    public Document setTotal(final Total total) {
        return new Document(processId, productName, charges, config, Optional.of(total), documentNumber, accountingId);
    }

    public record Total(long amount, String currency) {
    }

    public record ProcessConfig(
        Set<String> activitiesToFail,
        long numberCreationSeconds
    ) {
    }

    public record Charge(String code, long amount, String currency) {
    }
}
