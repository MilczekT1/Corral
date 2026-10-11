package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** MUST IGNORE: the raw parameter types are {@code Supplier} and {@code Optional[]}, the documented limit. */
public class AuditTrail {

    private final List<String> entries = new ArrayList<>();

    public void appendFrom(Supplier<Optional<String>> source) {
        source.get().ifPresent(entries::add);
    }

    @SafeVarargs
    public final void appendAll(Optional<String>... candidates) {
        for (Optional<String> candidate : candidates) {
            candidate.ifPresent(entries::add);
        }
    }
}
