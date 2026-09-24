package io.github.milczekt1.corral.rules.testing.nomutablestaticstate.fixtures;

import java.util.ArrayList;
import java.util.List;

/** MUST FLAG: a static collection field, reassignable and carried across every test in the fork. */
public class PriceCapturing {

    private static List<String> capturedPrices = new ArrayList<>();

    public List<String> capture(String price) {
        capturedPrices.add(price);
        return List.copyOf(capturedPrices);
    }
}
