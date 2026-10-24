package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.util.Objects;
import org.spockframework.util.Assert;

/** MUST FLAG {@code org.spockframework..}: Spock's internal precondition helper. */
public class ShippingQuote {

    int quote(String destination, int weightGrams) {
        Assert.notNull(destination);
        return Objects.hash(destination) % 100 + weightGrams / 1000;
    }
}
