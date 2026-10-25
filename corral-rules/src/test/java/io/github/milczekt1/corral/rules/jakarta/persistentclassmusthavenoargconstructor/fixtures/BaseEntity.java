package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/** MUST IGNORE: the provider never instantiates a mapped superclass. */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    private Long id;

    protected BaseEntity(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }
}
