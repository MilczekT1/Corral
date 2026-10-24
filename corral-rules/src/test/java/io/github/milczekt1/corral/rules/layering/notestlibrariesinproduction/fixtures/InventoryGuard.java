package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;

/** MUST FLAG {@code org.hamcrest..}: a Hamcrest assertion as a guard clause. */
public class InventoryGuard {

    int reserve(Map<String, Integer> stock, String sku) {
        assertThat(stock.get(sku), notNullValue());
        return stock.get(sku);
    }
}
