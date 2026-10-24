package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;

/**
 * MUST FLAG the class annotation and {@code customer}; MUST IGNORE the {@code JsonNode} column and the
 * {@code ObjectMapper} call, which are Jackson types but not annotations.
 */
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PurchaseOrder {

    @Id
    private Long id;

    @JsonIgnore
    @ManyToOne
    private Customer customer;

    @Column(columnDefinition = "jsonb")
    private JsonNode attributes;

    @Transient
    private String summary;

    @PostLoad
    void summarise() {
        summary = new ObjectMapper().createObjectNode().put("id", id).toString();
    }
}
