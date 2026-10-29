package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST IGNORE: an identity {@code equals} paired with identity hashing. */
public class IdentityRef {

    @Override
    public final boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
