package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Embeddable;

/** MUST FLAG: Hibernate reads {@code @EmbeddableInstantiator} only directly on the class, never through a composed annotation. */
@Embeddable
@HibernateBuilt
public class DiscountRate {

    private int basisPoints;

    public DiscountRate(int basisPoints) {
        this.basisPoints = basisPoints;
    }

    public int basisPoints() {
        return basisPoints;
    }
}
