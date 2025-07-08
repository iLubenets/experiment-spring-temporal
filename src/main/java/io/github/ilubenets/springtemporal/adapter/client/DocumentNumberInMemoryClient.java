package io.github.ilubenets.springtemporal.adapter.client;

import java.time.Duration;

import io.github.ilubenets.springtemporal.domain.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class DocumentNumberInMemoryClient
    implements DocumentNumberClient {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentNumberInMemoryClient.class);

    @Override
    public String generate(final String productName, final Document.ProcessConfig config) throws ClientException {
        LOG.info("Generating numbers for product {}", productName);
        try {
            Thread.sleep(Duration.ofSeconds(config.numberCreationSeconds()));

            var documentNumber = String.format("%d", System.currentTimeMillis());
            LOG.info("Generated number {} for product {}", documentNumber, productName);
            return documentNumber;
        } catch (InterruptedException e) {
            throw new ClientException(e.getMessage());
        }
    }

    @Override
    public void confirm(final String documentNumber) throws ClientException {
        LOG.info("Confirming document number {}", documentNumber);
        if (documentNumber == null) {
            throw new ClientException("Document number cannot be null or empty");
        }
        LOG.info("Confirmed document number {}", documentNumber);
    }

    @Override
    public void cancel(final String documentNumber) throws ClientException {
        LOG.info("Cancelling document number {}", documentNumber);
        if (documentNumber == null) {
            throw new ClientException("Document number cannot be null or empty");
        }
        LOG.info("Cancelled document number {}", documentNumber);
    }
}
