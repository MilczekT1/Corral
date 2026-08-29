package com.example.consumer.exclusions;

/**
 * Writes to stderr, which {@code corral.logging.no-system-err} forbids and this module excludes.
 *
 * <p>That rule is frozen clean here, so this class is a new violation: delete the line in
 * {@code src/test/resources/corral-exclusions.txt} and the build fails naming this class.
 *
 * <p>Outside {@code ..service..} so the consumer's own {@code acme.no-stdout-in-services} does not
 * fire on the same line.
 */
public class StderrWriterAllowedByExclusion {

    public void writeToStderr(String message) {
        System.err.println(message);
    }
}
