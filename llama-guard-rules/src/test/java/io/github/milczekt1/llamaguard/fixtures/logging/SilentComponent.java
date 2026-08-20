package io.github.milczekt1.llamaguard.fixtures.logging;

/** Touches neither stream. Both rules must leave it alone. */
public class SilentComponent {

    public String greet(String name) {
        return "hello " + name;
    }
}
