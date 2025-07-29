package io.github.ilubenets.springtemporal.domain;

import java.util.List;
import java.util.Optional;

public record Document(
    String processId,
    String orderId,
    ExampleType example,
    List<Charge> charges,
    Optional<Total> total,
    Optional<String> documentNumber,
    Optional<String> accountingId
) {

    public enum ExampleType {
        INCIDENT_EMBEDDED,
        INCIDENT_CHILD_WF,
    }

    public record Total(long amount, String currency) {
    }

    public record Charge(String code, long amount, String currency) {
    }

    public Document setDocumentNumber(final String number) {
        return new Document(processId, orderId, example, charges, total, Optional.of(number), accountingId);
    }

    public Document setAccountingId(final String id) {
        return new Document(processId, orderId, example, charges, total, documentNumber, Optional.of(id));
    }

    public Document setTotal(final Total total) {
        return new Document(processId, orderId, example, charges, Optional.of(total), documentNumber, accountingId);
    }
}
