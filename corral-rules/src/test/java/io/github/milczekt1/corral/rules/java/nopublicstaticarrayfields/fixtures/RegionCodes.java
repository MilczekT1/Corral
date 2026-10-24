package io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields.fixtures;

import java.util.List;

/**
 * MUST FLAG {@code ISO_CODES} and the zero-length {@code EMPTY}; MUST IGNORE the list, the private,
 * protected and package-private arrays, and the instance array.
 */
public class RegionCodes {

    public static final String[] ISO_CODES = {"EU", "US"};

    public static final String[] EMPTY = {};

    public static final List<String> REGIONS = List.of("Europe", "Americas");

    private static final String[] INTERNAL_PREFIXES = {"x-", "int-"};

    protected static final String[] LEGACY_ALIASES = {"EEC"};

    static final String[] PACKAGE_DEFAULTS = {"EU"};

    public final String[] localOverrides = {"PL"};

    public static boolean isInternal(String code) {
        for (String prefix : INTERNAL_PREFIXES) {
            if (code.startsWith(prefix)) {
                return true;
            }
        }
        return LEGACY_ALIASES.length + PACKAGE_DEFAULTS.length == 0;
    }
}
