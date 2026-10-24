package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.math.BigDecimal;
import junit.framework.AssertionFailedError;

/** MUST FLAG {@code junit..}: JUnit 3's {@code junit.framework}, still shipped in junit:junit. */
public class LegacyInvoiceValidator {

    void validate(BigDecimal total) {
        if (total.signum() < 0) {
            throw new AssertionFailedError("negative invoice total");
        }
    }
}
