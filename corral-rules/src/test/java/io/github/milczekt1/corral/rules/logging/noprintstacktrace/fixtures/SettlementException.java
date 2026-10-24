package io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures;

/**
 * MUST IGNORE: a project exception that is never printed; {@code printableSummary()} shares only the
 * {@code print} prefix, so a predicate matching the prefix fails here.
 */
public class SettlementException extends RuntimeException {

    public SettlementException(String message) {
        super(message);
    }

    public String printableSummary() {
        return "settlement failed: " + getMessage();
    }
}
