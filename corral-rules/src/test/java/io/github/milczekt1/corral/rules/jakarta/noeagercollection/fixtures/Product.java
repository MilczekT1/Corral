package io.github.milczekt1.corral.rules.jakarta.noeagercollection.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST IGNORE: a collection element with no mappings of its own. */
@Entity
public class Product {

    @Id
    private Long id;

    public Long id() {
        return id;
    }
}
