package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import com.google.common.truth.Truth;
import java.util.Map;

/** MUST FLAG {@code com.google.common.truth..}: a Truth assertion, from Guava's neighbour package. */
public class TaxRateTable {

    double rateFor(Map<String, Double> rates, String region) {
        Truth.assertThat(rates).containsKey(region);
        return rates.get(region);
    }
}
