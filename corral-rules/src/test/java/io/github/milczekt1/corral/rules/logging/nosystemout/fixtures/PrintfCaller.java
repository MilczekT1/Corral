package io.github.milczekt1.corral.rules.logging.nosystemout.fixtures;

/** MUST FLAG: {@code printf} and {@code print}, not println — only the field access catches them. */
public class PrintfCaller {

    public void report(String name) {
        System.out.printf("hello %s%n", name);
        System.out.print("done");
    }
}
