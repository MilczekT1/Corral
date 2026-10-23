package io.github.milczekt1.corral.rules.naming.lowercasepackages.fixtures.payout_processing;

/** MUST IGNORE: an underscore is not an uppercase letter. */
public class Payout {

    public long feeInCents(long amountInCents) {
        return Math.max(25, amountInCents / 100);
    }
}
