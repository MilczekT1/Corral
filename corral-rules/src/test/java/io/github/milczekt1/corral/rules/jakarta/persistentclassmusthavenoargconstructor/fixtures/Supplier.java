package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import javax.persistence.Entity;
import javax.persistence.Id;

/** MUST IGNORE: a package-private no-arg constructor is enough for Hibernate and its proxy. */
@Entity
public class Supplier {

    @Id
    private Long id;

    Supplier() {
    }

    public Supplier(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }
}
