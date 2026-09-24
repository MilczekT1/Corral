package io.github.milczekt1.corral.rules.testing.nomutablestaticstate.fixtures;

import java.util.ArrayList;
import java.util.List;

/**
 * MUST NOT FLAG. The shapes the predicate lets through, the mutable object behind {@code static final} among them:
 * it is ignored here because the rule genuinely does not see it, not because it is safe.
 */
public class BasketPricing {

    private static final String CURRENCY = "PLN";

    private static final List<String> KNOWN_SKUS = new ArrayList<>();

    private String basket = "";

    public String price(String sku) {
        KNOWN_SKUS.add(sku);
        basket = basket + sku;
        return basket + " " + CURRENCY;
    }
}
