package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.assertj.core.util.VisibleForTesting;

/** MUST FLAG {@code org.assertj..}: an auto-imported annotation is a dependency too. */
public class PriceCalculator {

    @VisibleForTesting
    BigDecimal round(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }
}
