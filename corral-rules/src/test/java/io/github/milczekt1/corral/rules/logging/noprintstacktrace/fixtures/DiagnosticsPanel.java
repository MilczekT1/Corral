package io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures;

import java.util.ArrayList;
import java.util.List;

/**
 * MUST IGNORE: declares and calls a {@code printStackTrace()} of its own on a type that is not a
 * {@code Throwable}, so a predicate dropping the owner fails here.
 */
public class DiagnosticsPanel {

    private final List<String> frames = new ArrayList<>();

    public void record(String frame) {
        frames.add(frame);
    }

    public String printStackTrace() {
        return String.join("\n", frames);
    }

    public String render() {
        return "Stack:\n" + printStackTrace();
    }
}
