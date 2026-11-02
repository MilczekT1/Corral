package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production;

import jakarta.validation.constraints.Positive;

/** MUST IGNORE: an interface is never bound itself. */
public interface RetryPolicy {
    @Positive
    int getMaxAttempts();
}
