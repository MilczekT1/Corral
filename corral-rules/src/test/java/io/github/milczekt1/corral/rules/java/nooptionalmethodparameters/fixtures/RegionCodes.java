package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import java.util.Optional;

/** MUST FLAG {@code parse} and {@code fallback}: static and private methods are checked like any other. */
public final class RegionCodes {

    private RegionCodes() {
    }

    public static String parse(Optional<String> raw) {
        return fallback(raw.map(String::trim));
    }

    private static String fallback(Optional<String> trimmed) {
        return trimmed.orElse("EU");
    }
}
