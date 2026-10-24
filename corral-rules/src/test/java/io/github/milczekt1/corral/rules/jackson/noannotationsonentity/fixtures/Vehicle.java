package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * MUST FLAG {@code @JsonTypeInfo} twice, its {@code defaultImpl} defaulting to an annotation, and
 * {@code @JsonSubTypes} twice, its nested {@code @Type} being one; MUST IGNORE the subtype class literal.
 */
@Entity
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({@JsonSubTypes.Type(value = Truck.class, name = "truck")})
public abstract class Vehicle {

    @Id
    private Long id;
}
