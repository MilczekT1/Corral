package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.math.BigDecimal;
import org.testng.Assert;

/** MUST FLAG {@code org.testng..}: a TestNG assertion in production code. */
public class CurrencyConverter {

    BigDecimal convert(BigDecimal amount, BigDecimal rate) {
        Assert.assertTrue(rate.signum() > 0, "rate must be positive");
        return amount.multiply(rate);
    }
}
