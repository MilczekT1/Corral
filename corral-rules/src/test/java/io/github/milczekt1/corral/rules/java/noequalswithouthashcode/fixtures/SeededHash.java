package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST IGNORE: declares no {@code equals}; its {@code hashCode(int)} overload is what {@code Barcode} inherits. */
public abstract class SeededHash {

    protected abstract String value();

    int hashCode(int seed) {
        return seed ^ value().hashCode();
    }
}
