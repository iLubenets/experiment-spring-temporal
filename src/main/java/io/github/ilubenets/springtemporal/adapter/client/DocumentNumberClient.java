package io.github.ilubenets.springtemporal.adapter.client;

import io.github.ilubenets.springtemporal.domain.Document;

public interface DocumentNumberClient {

    String generate(String productName, Document.ProcessConfig config) throws ClientException;

    void confirm(String documentNumber) throws ClientException;

    void cancel(String documentNumber) throws ClientException;
}
