package io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/** MUST FLAG the {@code Vector} parameter and the {@code Vector} type argument of the {@code Map} field. */
public class ReportRows {

    private final Map<String, Vector<String>> byRegion = new HashMap<>();

    public void load(String region, Vector<String> rows) {
        byRegion.put(region, rows);
    }
}
