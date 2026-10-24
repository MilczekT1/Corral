package io.github.milczekt1.corral.rules.java.noclassnewinstance.fixtures;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * MUST IGNORE: {@code Array.newInstance} and a {@code newInstance} method of its own, so a predicate
 * dropping the owner check fails here.
 */
public class SafeInstantiation {

    private final String label;

    private SafeInstantiation(String label) {
        this.label = label;
    }

    public static SafeInstantiation newInstance(String label) {
        return new SafeInstantiation(label);
    }

    public static SafeInstantiation unnamed() {
        return newInstance("unnamed");
    }

    public String[] labels(int size) {
        String[] labels = (String[]) Array.newInstance(String.class, size);
        Arrays.fill(labels, label);
        return labels;
    }
}
