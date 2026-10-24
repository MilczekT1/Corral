package io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures;

/** MUST IGNORE {@code super(message)} from a direct {@code Throwable} subclass. */
public class CallerTrace extends Throwable {

    public CallerTrace(String message) {
        super(message);
    }
}
