package io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/** MUST IGNORE: the replacement, the throwable handed to a logger with a message saying what failed. */
public class ChargeService {

    private static final Logger LOG = Logger.getLogger(ChargeService.class.getName());

    public boolean charge(Path card) {
        try {
            Files.readString(card);
            return true;
        } catch (IOException failure) {
            LOG.log(Level.SEVERE, "charge failed for " + card.getFileName(), failure);
            return false;
        }
    }
}
