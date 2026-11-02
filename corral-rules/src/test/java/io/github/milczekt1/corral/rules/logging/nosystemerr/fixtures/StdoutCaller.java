package io.github.milczekt1.corral.rules.logging.nosystemerr.fixtures;

/** MUST IGNORE: every shape of System.out write — a static initializer, {@code printf}, {@code println(int)}. */
public class StdoutCaller {

    static {
        System.out.println("loaded");
    }

    public void report(String name) {
        System.out.printf("hello %s%n", name);
    }

    public void report(int count) {
        System.out.println(count);
    }
}
