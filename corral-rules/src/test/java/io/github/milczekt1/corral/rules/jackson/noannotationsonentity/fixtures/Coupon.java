package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST FLAG {@code code}: a project annotation bundling Jackson annotations is a Jackson annotation. */
@Entity
public class Coupon {

    @Id
    private Long id;

    @WireName
    private String code;
}
