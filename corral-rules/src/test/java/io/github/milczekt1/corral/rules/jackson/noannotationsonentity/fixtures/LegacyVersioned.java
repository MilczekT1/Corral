package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.MappedSuperclass;

/** MUST FLAG {@code getVersion}: a javax mapped superclass, annotated on a getter. */
@MappedSuperclass
public abstract class LegacyVersioned {

    private long version;

    @JsonIgnore
    public long getVersion() {
        return version;
    }
}
