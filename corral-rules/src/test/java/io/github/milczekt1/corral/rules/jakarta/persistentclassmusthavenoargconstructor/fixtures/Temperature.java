package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.EmbeddableInstantiator;

/** MUST IGNORE: Hibernate builds it through the named instantiator. */
@Embeddable
@EmbeddableInstantiator(CelsiusReader.class)
public class Temperature {

    private double celsius;

    public Temperature(double celsius) {
        this.celsius = celsius;
    }

    public double celsius() {
        return celsius;
    }
}
