package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST FLAG: an identity {@code equals} keeps the contract, but the rule does not read the body. */
public class IdentityToken {

    @Override
    public final boolean equals(Object o) {
        return this == o;
    }
}
