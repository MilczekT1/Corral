package io.github.milczekt1.corral.rules.java.nobigdecimaldoubleconstructor.fixtures;

/**
 * MUST IGNORE: calls a constructor taking a {@code double} that is not {@code BigDecimal}'s, so a
 * predicate dropping the owner check fails here.
 */
public class ExchangeRate {

    private final double rate;

    public ExchangeRate(double rate) {
        this.rate = rate;
    }

    public static ExchangeRate of(double rate) {
        return new ExchangeRate(rate);
    }

    public double rate() {
        return rate;
    }
}
