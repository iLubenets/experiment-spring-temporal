package io.github.ilubenets.springtemporal.adapter.repository;

import java.util.Map;
import java.util.Optional;

import io.github.ilubenets.springtemporal.domain.Document;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentPostgresRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DocumentPostgresRepository(final NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void add(final Document document) {
        jdbc.update(
            "INSERT INTO document(id, data) VALUES (:id, :data::JSONB)",
            Map.of(
                "id", document.processId(),
                "data", PostgresqlObjectMapper.toJson(document)
            )
        );
    }

    public Optional<Document> get(final String id) {
        try {
            return Optional.of(
                jdbc.queryForObject("SELECT data FROM document WHERE id = :id",
                    Map.of("id", id),
                    (rs, rowNum) -> PostgresqlObjectMapper.fromJson(rs.getString("data"), Document.class))
            );
        } catch (final EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Document require(final String id) {
        return get(id).orElseThrow(() -> new IllegalStateException("Document with id " + id + " not found"));
    }

    public void update(final Document document) {
        jdbc.update(
            "UPDATE document SET data = :data::JSONB WHERE id = :id",
            Map.of(
                "id", document.processId(),
                "data", PostgresqlObjectMapper.toJson(document)
            )
        );
    }
}
