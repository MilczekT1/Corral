package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import java.util.ArrayList;
import java.util.Optional;

/** MUST FLAG {@code find}, once: javac adds a synthetic bridge {@code List find(Optional)} for the covariant return. */
public class ArchiveQuery extends OrderQuery {

    @Override
    public ArrayList<String> find(Optional<String> customerId) {
        return new ArrayList<>(super.find(customerId));
    }
}
