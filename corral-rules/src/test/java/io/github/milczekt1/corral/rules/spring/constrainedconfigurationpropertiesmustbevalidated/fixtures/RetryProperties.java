package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** FLAGGED: the only constraint is on a {@code get*} getter of an interface it implements. */
@ConfigurationProperties("retry")
public class RetryProperties implements RetryPolicy {
    private int maxAttempts;

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }
}
