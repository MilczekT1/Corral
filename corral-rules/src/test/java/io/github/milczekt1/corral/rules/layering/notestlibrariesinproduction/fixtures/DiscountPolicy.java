package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import io.kotest.matchers.ShouldKt;
import java.math.BigDecimal;

/** MUST FLAG {@code io.kotest..}: a Kotest matcher called from Java production code. */
public class DiscountPolicy {

    BigDecimal apply(BigDecimal price, BigDecimal rate) {
        ShouldKt.shouldBe(rate.compareTo(BigDecimal.ONE) <= 0, true);
        return price.subtract(price.multiply(rate));
    }
}
