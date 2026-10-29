package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST IGNORE: an interface re-declaring {@code equals} for documentation, as {@code Comparator} does. */
public interface EqualityContract {

    @Override
    boolean equals(Object o);
}
