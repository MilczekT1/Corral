package io.github.milczekt1.corral.rules.testsupport;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;

/**
 * Checks a published rule against the store committed under {@code src/test/resources/archunit/frozen}.
 * Reseed with {@code -Darchunit.freeze.store.default.allowStoreCreation=true}, then commit.
 */
@UtilityClass
public class CommittedStore {

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String PATH = "src/test/resources/archunit/frozen";

    /**
     * Checks {@code published} against the committed store and asserts the index files its debt
     * under {@code id}.
     *
     * <p>The store goes in through {@link FreezingArchRule#persistIn}. Do not set {@code freeze.store}
     * instead: a frozen rule captures its store when constructed, which is class initialisation, so
     * that races class loading. Only the path goes on the process-wide {@link ArchConfiguration}.
     *
     * @return the debt recorded under {@code id}
     */
    public static String freeze(ArchRule published, String id, JavaClasses examples) throws IOException {
        ArchConfiguration.get().setProperty("freeze.store.default.path", PATH);
        try {
            assertInstanceOf(FreezingArchRule.class, published,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore())
                    .check(examples);
        } finally {
            ArchConfiguration.get().reset();
        }

        String index = Files.readString(Path.of(PATH, "stored.rules"));
        assertTrue(index.contains(id + "=" + id), "the index must file this rule's debt under its id: " + index);

        return Files.readString(Path.of(PATH, id));
    }
}
