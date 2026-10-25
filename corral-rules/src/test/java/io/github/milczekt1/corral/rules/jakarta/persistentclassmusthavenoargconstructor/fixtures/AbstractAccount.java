package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST FLAG: abstract entities need it too, for the lazy proxy of the base type. */
@Entity
public abstract class AbstractAccount {

    @Id
    private Long id;

    protected AbstractAccount(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }
}
