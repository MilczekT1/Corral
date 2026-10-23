package io.github.milczekt1.corral.rules.naming.lowercasepackages.fixtures.int_;

/** MUST IGNORE: a keyword escaped with an underscore, as the JLS suggests. */
public class Ledger {

    public long balance(long credits, long debits) {
        return credits - debits;
    }
}
