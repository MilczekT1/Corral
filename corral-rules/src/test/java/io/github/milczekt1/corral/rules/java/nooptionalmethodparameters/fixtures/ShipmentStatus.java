package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import java.util.Optional;

/** MUST FLAG {@code describe}: an enum's methods are checked like any class's. */
public enum ShipmentStatus {
    PENDING,
    DELIVERED;

    public String describe(Optional<String> note) {
        return name() + note.map(text -> ": " + text).orElse("");
    }
}
