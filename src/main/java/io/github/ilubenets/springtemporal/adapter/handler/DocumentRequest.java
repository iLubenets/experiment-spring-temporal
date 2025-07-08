package io.github.ilubenets.springtemporal.adapter.handler;

import java.util.List;
import java.util.Set;

public record DocumentRequest(
    String orderId,
    String productName,
    List<Charge> charges,
    ProcessConfig config
) {

    public record ProcessConfig(
        Set<String> activitiesToFail,
        long numberCreationSeconds
    ) {
    }

    public record Charge(String code, long amount, String currency) {
    }
}
