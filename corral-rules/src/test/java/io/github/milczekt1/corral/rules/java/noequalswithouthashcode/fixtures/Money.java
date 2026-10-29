package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST FLAG: equal values hash by identity. */
public final class Money {

    private final long cents;

    public Money(long cents) {
        this.cents = cents;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money other && other.cents == cents;
    }
}
