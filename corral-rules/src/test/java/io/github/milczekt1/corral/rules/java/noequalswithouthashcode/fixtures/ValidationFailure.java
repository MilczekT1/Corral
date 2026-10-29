package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST FLAG: neither {@code Exception} nor {@code Throwable} declares {@code hashCode()}. */
public class ValidationFailure extends Exception {

    private final String field;

    public ValidationFailure(String field) {
        super("invalid " + field);
        this.field = field;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ValidationFailure other && other.field.equals(field);
    }
}
