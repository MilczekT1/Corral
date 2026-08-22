package io.github.milczekt1.corral.fixtures.logging;

/** Writes on class load rather than from a method, and is reported as a static initializer. */
public class StaticInitializerPrinter {

    static {
        System.out.println("loaded");
    }

    public void doNothing() {
    }
}
