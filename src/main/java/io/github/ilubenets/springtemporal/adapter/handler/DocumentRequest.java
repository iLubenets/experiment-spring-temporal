package io.github.ilubenets.springtemporal.adapter.handler;

import io.github.ilubenets.springtemporal.domain.Document;

import java.util.List;
import java.util.Set;

public record DocumentRequest(
    String orderId,
    Document.ExampleType example,
    List<Charge> charges
) {

    public record ProcessConfig(
        Set<String> activitiesToFail,
        long numberCreationSeconds
    ) {
    }

    public record Charge(String code, long amount, String currency) {
    }
}
