package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST IGNORE: an abstract {@code equals} forces subclasses to implement it. */
public abstract class AbstractValue {

    @Override
    public abstract boolean equals(Object o);
}
