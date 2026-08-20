package io.github.milczekt1.llamaguard.fixtures.logging;

/** Touches System.err only, so it is also the proof that the two rules do not overlap. */
public class StderrCaller {

    public void report(Exception failure) {
        System.err.print("failed: ");
        System.err.println(failure.getMessage());
    }
}
