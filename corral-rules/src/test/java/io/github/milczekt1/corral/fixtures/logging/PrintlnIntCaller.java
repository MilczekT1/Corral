package io.github.milczekt1.corral.fixtures.logging;

/** The overload the narrow "callMethod(println, String.class)" rule misses entirely. */
public class PrintlnIntCaller {

    public void report(int count) {
        System.out.println(count);
    }
}
