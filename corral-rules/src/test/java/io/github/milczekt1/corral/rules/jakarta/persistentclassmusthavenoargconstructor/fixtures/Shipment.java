package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST FLAG: the only no-arg constructor is private, which leaves no lazy proxy. */
@Entity
public class Shipment {

    @Id
    private Long id;

    private Shipment() {
        this.id = 0L;
    }

    public Shipment(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }
}
