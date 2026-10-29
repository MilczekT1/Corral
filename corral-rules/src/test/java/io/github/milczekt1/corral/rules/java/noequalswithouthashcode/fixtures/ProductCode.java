package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST FLAG: {@code hashCode(int)} is an overload, not the {@code hashCode()} hash collections call. */
public final class ProductCode {

    private final String value;

    public ProductCode(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ProductCode other && other.value.equals(value);
    }

    int hashCode(int seed) {
        return seed ^ value.hashCode();
    }
}
