package io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation.fixtures;

import javax.naming.InvalidNameException;

/** MUST IGNORE: {@code javax.naming} is a JDK {@code javax} package, so a predicate widened to {@code javax..} fails. */
public class HandCheckedRefund {

    public void validate(String reference) throws InvalidNameException {
        if (reference == null || reference.isBlank()) {
            throw new InvalidNameException("reference is required");
        }
    }
}
