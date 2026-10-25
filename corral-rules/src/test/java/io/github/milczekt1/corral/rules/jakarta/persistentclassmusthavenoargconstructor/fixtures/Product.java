package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST IGNORE: no constructor declared, so javac emits a public no-arg one. */
@Entity
public class Product {

    @Id
    private Long id;

    public Long id() {
        return id;
    }
}
