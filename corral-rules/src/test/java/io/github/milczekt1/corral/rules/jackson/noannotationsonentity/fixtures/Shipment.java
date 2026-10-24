package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

/** MUST FLAG {@code trackingNumber}: Jackson 3 moved the databind annotations to {@code tools.jackson}. */
@Entity
public class Shipment {

    @Id
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long trackingNumber;
}
