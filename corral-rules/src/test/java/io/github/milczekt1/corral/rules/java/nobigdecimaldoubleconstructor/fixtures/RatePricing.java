package io.github.milczekt1.corral.rules.java.nobigdecimaldoubleconstructor.fixtures;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * MUST FLAG the static initializer, {@code fromField}, {@code fromFloat} and {@code withContext}: each
 * calls a {@code double}-first {@code BigDecimal} constructor, {@code fromFloat} through the compiler
 * widening {@code 0.1f}. MUST IGNORE {@code fromText}: the {@code String} constructor beside them, so a
 * predicate dropping the parameter check fails here.
 */
public class RatePricing {

    static final BigDecimal RATE = new BigDecimal(0.1);

    private final double discount;

    public RatePricing(double discount) {
        this.discount = discount;
    }

    public BigDecimal fromField() {
        return new BigDecimal(discount);
    }

    public BigDecimal fromFloat() {
        return new BigDecimal(0.1f);
    }

    public BigDecimal withContext(double rate) {
        return new BigDecimal(rate, MathContext.DECIMAL64);
    }

    public BigDecimal fromText() {
        return new BigDecimal("0.1").add(RATE);
    }
}
