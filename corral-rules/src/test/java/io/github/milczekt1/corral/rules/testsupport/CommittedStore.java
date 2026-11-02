package io.github.milczekt1.corral.rules.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * The store is read-only here: a new entry, or a predicate that stops matching a recorded line, fails
 * instead of rewriting it. Reseed with
 * {@code -Darchunit.freeze.store.default.allowStoreCreation=true -Darchunit.freeze.store.default.allowStoreUpdate=true},
 * then commit.
 */
@UtilityClass
public class CommittedStore {

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String PATH = "src/test/resources/archunit/frozen";

    /**
     * Checks {@code published} against the committed store and asserts it is filed under {@code id}.
     *
     * <p>The store goes in through {@link FreezingArchRule#persistIn}. Do not set {@code freeze.store}
     * instead: a frozen rule captures its store when constructed, which is class initialisation, so
     * that races class loading.
     *
     * @return the debt recorded under {@code id}
     */
    public static String freeze(ArchRule published, String id, JavaClasses examples) throws IOException {
        assertEquals(id, published.getDescription(), "the published rule must be named after its id");

        ArchConfiguration.withThreadLocalScope(configuration -> {
            configuration.setProperty("freeze.store.default.path", PATH);
            configuration.setProperty("freeze.store.default.allowStoreUpdate", "false");
            assertInstanceOf(FreezingArchRule.class, published,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore())
                    .check(examples);
        });

        String index = Files.readString(Path.of(PATH, "stored.rules"));
        assertTrue(index.contains(id + "=" + id), "the index must file this rule's debt under its id: " + index);

        return Files.readString(Path.of(PATH, id));
    }
}
