package io.github.milczekt1.corral.rules.java.nobigdecimaldoubleconstructor.fixtures;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * MUST IGNORE: every {@code BigDecimal} here is built without a {@code double} constructor, and
 * {@code total} is a field it never constructs. {@code valueOf(double)} takes a {@code double} first
 * but is a method, so a predicate dropping the constructor-name check fails here.
 */
public class SafePricing {

    private BigDecimal total;

    public BigDecimal rounded(double rate) {
        return BigDecimal.valueOf(rate).round(MathContext.DECIMAL64);
    }

    public BigDecimal literal() {
        return BigDecimal.valueOf(0.1);
    }

    public BigDecimal parsed() {
        return new BigDecimal("0.1");
    }

    public BigDecimal floatAsText(float rate) {
        return new BigDecimal(Float.toString(rate));
    }

    public BigDecimal counted(int units, long cents) {
        return new BigDecimal(units).add(new BigDecimal(cents));
    }

    public BigDecimal total() {
        return total;
    }
}
