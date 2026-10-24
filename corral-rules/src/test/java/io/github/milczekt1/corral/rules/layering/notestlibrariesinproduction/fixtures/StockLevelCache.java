package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.ConcurrentHashMap;

/** MUST IGNORE: Guava's {@code @VisibleForTesting} ships in production Guava, beside Truth's package. */
public class StockLevelCache {

    private final ConcurrentHashMap<String, Integer> levels = new ConcurrentHashMap<>();

    @VisibleForTesting
    int size() {
        return levels.size();
    }
}
