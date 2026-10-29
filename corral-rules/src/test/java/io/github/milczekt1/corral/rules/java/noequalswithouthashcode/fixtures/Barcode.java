package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST FLAG: a superclass's {@code hashCode(int)} overload is not the {@code hashCode()} hash collections call. */
public final class Barcode extends SeededHash {

    private final String digits;

    public Barcode(String digits) {
        this.digits = digits;
    }

    @Override
    protected String value() {
        return digits;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Barcode other && other.digits.equals(digits);
    }
}
