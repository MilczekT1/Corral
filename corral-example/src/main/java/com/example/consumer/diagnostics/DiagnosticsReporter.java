package com.example.consumer.diagnostics;

/**
 * Writes to stderr on purpose, to demonstrate {@code corral-exclusions.txt}.
 *
 * <p>{@code logging.no-system-err} is frozen clean in this module's store, so this is a NEW
 * violation and would fail the build. The committed exclusion is what keeps it green — delete that
 * one line and watch the rule bite.
 *
 * <p>Outside {@code ..service..} deliberately, so the consumer's own {@code acme.no-stdout-in-services}
 * rule does not fire on the same line and blur what is being demonstrated.
 */
public class DiagnosticsReporter {

    public void report(String message) {
        System.err.println(message);
    }
}
