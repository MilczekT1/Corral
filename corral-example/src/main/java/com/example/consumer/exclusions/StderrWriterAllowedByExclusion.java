package com.example.consumer.exclusions;

/**
 * Writes to stderr, which {@code corral.logging.no-system-err} forbids — and which this module excludes.
 *
 * <p>The name is the point. That rule is frozen <em>clean</em> in this module's store, so this class
 * is a NEW violation and fails the build on its own. The one line in
 * {@code src/test/resources/corral-exclusions.txt} is the only reason it does not. Delete that line
 * and the failure names this class, which then reads as exactly what it is: a writer that was
 * allowed by an exclusion which is no longer there.
 *
 * <p>Outside {@code ..service..} deliberately, so the consumer's own
 * {@code acme.no-stdout-in-services} rule does not fire on the same line and blur which rule is
 * being demonstrated.
 */
public class StderrWriterAllowedByExclusion {

    public void writeToStderr(String message) {
        System.err.println(message);
    }
}
