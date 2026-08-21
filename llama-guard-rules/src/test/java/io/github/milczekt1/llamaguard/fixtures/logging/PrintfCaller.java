package io.github.milczekt1.llamaguard.fixtures.logging;

/** Not println at all — only the field access catches this. */
public class PrintfCaller {

    public void report(String name) {
        System.out.printf("hello %s%n", name);
        System.out.print("done");
    }
}
