package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Instantiator;

/** MUST FLAG: the {@code @Instantiator} constructor is public but the class is not, and Hibernate calls it without setAccessible. */
@Embeddable
class Distance {

    private int metres;

    @Instantiator("metres")
    public Distance(int metres) {
        this.metres = metres;
    }

    int metres() {
        return metres;
    }
}
