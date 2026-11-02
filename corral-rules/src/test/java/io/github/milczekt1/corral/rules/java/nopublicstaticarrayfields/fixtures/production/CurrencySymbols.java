package io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields.fixtures.production;

/** MUST FLAG the array {@code SYMBOLS}, implicitly {@code public static final}; MUST IGNORE {@code DEFAULT_CURRENCY}. */
public interface CurrencySymbols {

    String[] SYMBOLS = {"€", "$"};

    String DEFAULT_CURRENCY = "EUR";
}
