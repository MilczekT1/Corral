package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST IGNORE: a protected no-arg constructor beside the invariant-enforcing one. */
@Entity
public class Payment {

    @Id
    private Long id;

    protected Payment() {
    }

    public Payment(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }
}
