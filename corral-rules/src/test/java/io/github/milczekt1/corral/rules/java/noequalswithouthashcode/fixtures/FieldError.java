package io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures;

/** MUST IGNORE: inherits {@code equals} without declaring it, so only {@code ValidationFailure} is reported. */
public class FieldError extends ValidationFailure {

    public FieldError(String field) {
        super(field);
    }
}
