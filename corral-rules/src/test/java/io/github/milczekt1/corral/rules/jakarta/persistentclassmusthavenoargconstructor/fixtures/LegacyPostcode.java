package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import javax.persistence.Embeddable;
import org.hibernate.annotations.Instantiator;

/** MUST FLAG: {@code @Instantiator} needs Hibernate 6.2+, which reads only {@code jakarta}. */
@Embeddable
public class LegacyPostcode {

    private String value;

    @Instantiator("value")
    public LegacyPostcode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
