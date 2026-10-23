package io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures;

/** MUST FLAG the {@code PrintStream} overload, which is no more managed for writing to stdout. */
public class ConsoleDumper {

    public void dump(IllegalStateException failure) {
        failure.printStackTrace(System.out);
        System.out.flush();
    }
}
