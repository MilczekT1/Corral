package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import java.util.Comparator;
import java.util.Optional;

/** MUST FLAG {@code compare}, once: the bridge method javac adds for it takes {@code Object}. */
public class PresenceComparator implements Comparator<Optional<String>> {

    @Override
    public int compare(Optional<String> left, Optional<String> right) {
        return Boolean.compare(left.isPresent(), right.isPresent());
    }
}
