package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures.production;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST IGNORE: an entity with no Jackson dependency at all. */
@Entity
public class Customer {

    @Id
    private Long id;

    private String name;
}
