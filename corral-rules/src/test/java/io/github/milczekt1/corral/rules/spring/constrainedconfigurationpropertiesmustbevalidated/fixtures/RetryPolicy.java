package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures;

import jakarta.validation.constraints.Positive;

/** MUST IGNORE: an interface, never bound itself. */
public interface RetryPolicy {
    @Positive
    int getMaxAttempts();
}
