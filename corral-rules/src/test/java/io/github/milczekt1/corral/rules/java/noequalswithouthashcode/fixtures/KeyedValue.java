package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST IGNORE: a concrete {@code equals} with an abstract {@code hashCode()} every subclass must implement. */
public abstract class KeyedValue {

    protected abstract String key();

    @Override
    public boolean equals(Object o) {
        return o instanceof KeyedValue other && other.key().equals(key());
    }

    @Override
    public abstract int hashCode();
}
