package io.github.milczekt1.corral.rules.testing.nomutablestaticstate.fixtures;

import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;

/** MUST NOT FLAG: JUnit assigns the field, so it cannot be final — the one shape the predicate exempts. */
public class TemporaryDirectoryHolder {

    @TempDir
    static Path exportDirectory;

    public Path exportDirectory() {
        return exportDirectory;
    }
}
