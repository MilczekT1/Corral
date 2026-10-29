package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** FLAGGED: the only constraint is on an {@code is*} getter. */
@ConfigurationProperties("consistency")
public class ConsistencyProperties {
    private int replicas;

    @AssertTrue
    public boolean isConsistent() {
        return replicas > 2;
    }
}
