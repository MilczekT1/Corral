package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures;

/** MUST IGNORE: a final class with no transactional members. */
public final class Money {

    private final long cents;

    public Money(long cents) {
        this.cents = cents;
    }

    public Money plus(Money other) {
        return new Money(cents + other.cents);
    }
}
