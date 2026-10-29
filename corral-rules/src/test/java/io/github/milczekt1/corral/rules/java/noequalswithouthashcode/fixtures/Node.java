package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST IGNORE: identity equality with any fixed hash keeps the contract. */
public final class Node {

    @Override
    public int hashCode() {
        return 42;
    }
}
