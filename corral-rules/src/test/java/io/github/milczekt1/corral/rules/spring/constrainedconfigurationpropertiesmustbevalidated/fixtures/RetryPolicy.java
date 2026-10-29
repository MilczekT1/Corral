package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.Positive;

/** NOT FLAGGED: an interface is never bound itself. */
public interface RetryPolicy {
    @Positive
    int getMaxAttempts();
}
