package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;

/** MUST FLAG: {@code transient} keeps the field out of the stream, but it still comes back {@code null}. */
public class CachedQuote implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient Optional<BigDecimal> discount = Optional.empty();

    public BigDecimal discountOrZero() {
        return discount.orElse(BigDecimal.ZERO);
    }
}
