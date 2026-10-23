package io.github.milczekt1.corral.rules.naming.lowercasepackages.fixtures.Model;

/** MUST FLAG: a capitalised last segment. */
public class Invoice {

    public long totalInCents(long netInCents, int taxPercent) {
        return netInCents + netInCents * taxPercent / 100;
    }
}
