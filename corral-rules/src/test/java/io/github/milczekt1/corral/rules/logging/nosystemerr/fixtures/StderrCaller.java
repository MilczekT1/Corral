package io.github.milczekt1.corral.rules.logging.nosystemerr.fixtures;

/** MUST FLAG: {@code print} and {@code println} on System.err. */
public class StderrCaller {

    public void report(Exception failure) {
        System.err.print("failed: ");
        System.err.println(failure.getMessage());
    }
}
