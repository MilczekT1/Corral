package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import java.util.Optional;

/** MUST IGNORE: a static constant is neither serialized nor left unassigned. */
public final class Defaults {

    public static final Optional<String> REGION = Optional.empty();

    private Defaults() {
    }

    public static String regionOrGlobal() {
        return REGION.orElse("global");
    }
}
