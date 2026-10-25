package io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST IGNORE: an association target with no associations of its own. */
@Entity
public class Merchant {

    @Id
    private Long id;

    public Long id() {
        return id;
    }
}
