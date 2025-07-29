package io.github.ilubenets.springtemporal.application;

import io.temporal.common.SearchAttributeKey;

public final class SearchAttributeKeys {

    public static final SearchAttributeKey<String> STATE = SearchAttributeKey.forKeyword("State");
    public static final SearchAttributeKey<String> ORDER_ID = SearchAttributeKey.forText("OrderId");

    public enum State {
        NEW,
        RUNNING,
        COMPLETED,
        INCIDENT,
    }
}
