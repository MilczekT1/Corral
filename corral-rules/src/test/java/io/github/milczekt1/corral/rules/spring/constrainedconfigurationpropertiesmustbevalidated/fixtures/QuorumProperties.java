package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** FLAGGED: the only constraint is on a {@code has*} getter, which Hibernate Validator reads. */
@ConfigurationProperties("quorum")
public class QuorumProperties {
    private int voters;

    @AssertTrue
    public boolean hasQuorum() {
        return voters >= 3;
    }
}
