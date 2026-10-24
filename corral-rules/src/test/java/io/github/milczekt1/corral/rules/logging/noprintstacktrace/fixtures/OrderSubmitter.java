package io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MUST FLAG {@code failure.printStackTrace()} in a catch block. MUST IGNORE {@code failure.getMessage()}:
 * same owner, so a predicate dropping the name fails here.
 */
public class OrderSubmitter {

    public String submit(Path order) {
        try {
            return Files.readString(order);
        } catch (IOException failure) {
            failure.printStackTrace();
            return failure.getMessage();
        }
    }
}
