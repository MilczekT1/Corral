package io.github.milczekt1.corral.rules.naming.lowercasepackages.fixtures.refund.processing;

/** MUST IGNORE: words split into lowercase segments. */
public class Refund {

    public long remainingInCents(long paidInCents, long refundedInCents) {
        return paidInCents - refundedInCents;
    }
}
