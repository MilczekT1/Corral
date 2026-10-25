package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** MUST FLAG: the only constraint is on an {@code is*} getter. */
@ConfigurationProperties("consistency")
public class ConsistencyProperties {
    private int readQuorum;
    private int writeQuorum;

    @AssertTrue
    public boolean isConsistent() {
        return readQuorum + writeQuorum > 3;
    }
}
