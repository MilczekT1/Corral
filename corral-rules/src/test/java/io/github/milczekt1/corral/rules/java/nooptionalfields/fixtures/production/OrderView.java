package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures.production;

import java.util.Optional;

/** MUST FLAG {@code note}, once, on its field; MUST IGNORE {@code id}. */
public record OrderView(String id, Optional<String> note) {
}
