package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST IGNORE: a {@code jsonb} column is persistence, not wire contract. */
@Entity
public class Settlement {

    @Id
    private Long id;

    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;
}
