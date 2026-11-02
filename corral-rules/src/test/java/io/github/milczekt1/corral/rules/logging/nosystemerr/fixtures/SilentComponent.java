package io.github.milczekt1.corral.rules.logging.nosystemerr.fixtures;

/** MUST IGNORE: touches neither stream. */
public class SilentComponent {

    public String greet(String name) {
        return "hello " + name;
    }
}
