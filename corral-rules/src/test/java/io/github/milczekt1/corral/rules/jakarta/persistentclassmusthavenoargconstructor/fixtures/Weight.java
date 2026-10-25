package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Instantiator;

/** MUST FLAG: Hibernate calls an {@code @Instantiator} constructor without setAccessible, so a protected one throws on load. */
@Embeddable
public class Weight {

    private int grams;

    @Instantiator("grams")
    protected Weight(int grams) {
        this.grams = grams;
    }

    public int grams() {
        return grams;
    }
}
