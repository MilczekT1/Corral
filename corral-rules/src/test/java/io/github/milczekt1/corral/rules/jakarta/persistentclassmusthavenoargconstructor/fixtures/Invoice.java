package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST FLAG: only an all-args constructor, so the provider cannot instantiate it on load. */
@Entity
public class Invoice {

    @Id
    private Long id;

    public Invoice(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }
}
