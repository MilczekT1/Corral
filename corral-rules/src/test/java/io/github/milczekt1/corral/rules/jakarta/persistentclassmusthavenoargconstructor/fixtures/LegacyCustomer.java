package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import javax.persistence.Entity;
import javax.persistence.Id;

/** MUST FLAG: a {@code javax} entity with only an all-args constructor. */
@Entity
public class LegacyCustomer {

    @Id
    private Long id;

    public LegacyCustomer(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }
}
