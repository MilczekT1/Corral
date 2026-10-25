package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import javax.persistence.Embeddable;
import org.hibernate.annotations.EmbeddableInstantiator;

/** MUST FLAG: {@code @EmbeddableInstantiator} needs Hibernate 6, which reads only {@code jakarta}. */
@Embeddable
@EmbeddableInstantiator(CelsiusReader.class)
public class LegacyPhoneNumber {

    private String digits;

    public LegacyPhoneNumber(String digits) {
        this.digits = digits;
    }

    public String digits() {
        return digits;
    }
}
