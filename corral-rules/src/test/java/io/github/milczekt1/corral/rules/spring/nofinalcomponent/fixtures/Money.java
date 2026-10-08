package io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures;

/** MUST IGNORE: final, but not a bean. */
public final class Money {

    private final long cents;

    public Money(long cents) {
        this.cents = cents;
    }

    public Money plus(Money other) {
        return new Money(cents + other.cents);
    }
}
