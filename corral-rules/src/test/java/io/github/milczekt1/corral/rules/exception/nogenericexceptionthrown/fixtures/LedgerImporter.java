package io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures;

import java.nio.file.Files;
import java.nio.file.Path;

/** MUST FLAG {@code new Exception}, a checked failure no caller can select on. */
public class LedgerImporter {

    public String importFile(Path ledger) throws Exception {
        if (!Files.isReadable(ledger)) {
            throw new Exception("unreadable ledger: " + ledger);
        }
        return Files.readString(ledger);
    }
}
