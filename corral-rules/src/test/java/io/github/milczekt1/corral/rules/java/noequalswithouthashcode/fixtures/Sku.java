package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST IGNORE: both declared. */
public final class Sku {

    private final String code;

    public Sku(String code) {
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Sku other && other.code.equals(code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
