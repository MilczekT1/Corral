package io.github.milczekt1.corral.rules.logging.nosystemout.fixtures;

/** MUST FLAG: {@code println(int)}, the overload a call match on {@code println(String)} misses. */
public class PrintlnIntCaller {

    public void report(int count) {
        System.out.println(count);
    }
}
