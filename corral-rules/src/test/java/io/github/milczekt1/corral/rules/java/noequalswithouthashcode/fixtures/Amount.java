package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST IGNORE: {@code equals(Amount)} is an overload, not an override of {@code equals(Object)}. */
public final class Amount {

    private final long value;

    public Amount(long value) {
        this.value = value;
    }

    boolean equals(Amount other) {
        return other.value == value;
    }
}
