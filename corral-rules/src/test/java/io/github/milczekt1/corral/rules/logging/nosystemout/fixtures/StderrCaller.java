package io.github.milczekt1.corral.rules.logging.nosystemout.fixtures;

/** MUST IGNORE: touches System.err only. */
public class StderrCaller {

    public void report(Exception failure) {
        System.err.print("failed: ");
        System.err.println(failure.getMessage());
    }
}
