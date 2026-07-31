package io.github.milczekt1.archrules.freeze;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.lang.ArchRule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives {@link EmptyOmittingViolationStore} directly rather than through {@code FreezingArchRule},
 * so each behaviour is isolated. None of these tests touch {@code ArchConfiguration}'s global
 * properties: the store under test is constructed and initialized with a local {@link Properties}
 * instance, so there is nothing global to reset in {@code @AfterEach}.
 */
class EmptyOmittingViolationStoreTest {

    @TempDir
    Path storeDir;

    private EmptyOmittingViolationStore store;

    private static ArchRule ruleNamed(String description) {
        return classes().should().haveSimpleName("Whatever").as(description).allowEmptyShould(true);
    }

    @BeforeEach
    void initStore() {
        Properties properties = new Properties();
        properties.setProperty("default.path", storeDir.toString());
        properties.setProperty("default.allowStoreCreation", "true");
        properties.setProperty("default.allowStoreUpdate", "true");
        store = new EmptyOmittingViolationStore();
        store.initialize(properties);
    }

    @Test
    void aCleanRuleIsRecordedInTheIndexButLeavesNoFile() throws Exception {
        ArchRule rule = ruleNamed("test.clean-rule");

        store.save(rule, List.of());

        assertTrue(store.contains(rule), "a clean rule must still be frozen");
        String index = Files.readString(storeDir.resolve("stored.rules"));
        assertTrue(index.contains("test.clean-rule="), "index entry must survive: " + index);
        assertEquals(0, countViolationFiles(), "a clean rule must leave no violation file");
    }

    @Test
    void aCleanRuleReadsBackAsZeroViolations() {
        ArchRule rule = ruleNamed("test.clean-rule");
        store.save(rule, List.of());

        assertEquals(List.of(), store.getViolations(rule));
    }

    @Test
    void aViolatingRuleKeepsItsFile() throws Exception {
        ArchRule rule = ruleNamed("test.dirty-rule");

        store.save(rule, List.of("Class <Foo> is bad"));

        assertEquals(List.of("Class <Foo> is bad"), store.getViolations(rule));
        assertEquals(1, countViolationFiles(), "violations must be written to a file");
    }

    @Test
    void aRuleThatBecomesCleanLosesItsFileButKeepsItsEntry() throws Exception {
        ArchRule rule = ruleNamed("test.was-dirty");
        store.save(rule, List.of("Class <Foo> is bad"));

        store.save(rule, List.of());

        assertTrue(store.contains(rule));
        assertEquals(List.of(), store.getViolations(rule));
        assertEquals(0, countViolationFiles());
    }

    @Test
    void storedRulesMapsRuleDescriptionToFileName() throws Exception {
        // Pins an assumption about TextFileBasedViolationStore's file layout that this decorator
        // depends on. If an ArchUnit upgrade changes it, fail here rather than silently mishandling
        // a consumer's store.
        ArchRule rule = ruleNamed("test.layout-probe");
        store.save(rule, List.of("Class <Foo> is bad"));

        Properties index = new Properties();
        try (var in = Files.newInputStream(storeDir.resolve("stored.rules"))) {
            index.load(in);
        }
        String fileName = index.getProperty("test.layout-probe");

        assertNotNull(fileName, "stored.rules must key violations by rule description");
        assertTrue(Files.exists(storeDir.resolve(fileName)),
                "stored.rules value must name a file in the store directory");
    }

    @Test
    void initializeFallsBackToArchUnitsOwnDefaultPathWhenDefaultPathIsOmitted() throws IOException {
        // Deliberately independent of the storeDir @TempDir fixture used by every other test in this
        // class: this exercises the fallback that applies when a consumer omits default.path
        // entirely (the case that used to throw NullPointerException), not the configured path.
        // TextFileBasedViolationStore.initialize() creates its "archunit_store" directory as a side
        // effect of resolving the path, regardless of whether store creation later succeeds, so we
        // only assert that initialize() completes and then remove that directory again rather than
        // asserting anything about its contents.
        Path defaultStoreDir = Path.of("archunit_store");
        try {
            Properties properties = new Properties();
            properties.setProperty("default.allowStoreCreation", "true");
            properties.setProperty("default.allowStoreUpdate", "true");
            // default.path deliberately omitted.

            EmptyOmittingViolationStore defaultPathStore = new EmptyOmittingViolationStore();

            assertDoesNotThrow(() -> defaultPathStore.initialize(properties));
        } finally {
            deleteRecursively(defaultStoreDir);
        }
    }

    /**
     * Counts rule violation files only, excluding the {@code stored.rules} index itself — chosen
     * over counting the index as a rule-violation file because it lets the assertions read as plain
     * violation-file counts (0, 1) rather than "index plus N".
     */
    private long countViolationFiles() throws IOException {
        try (var entries = Files.list(storeDir)) {
            return entries.filter(path -> !path.getFileName().toString().equals("stored.rules")).count();
        }
    }

    /** Cleanup for the stray directory ArchUnit's own store creates as a side effect of {@link #initializeFallsBackToArchUnitsOwnDefaultPathWhenDefaultPathIsOmitted}. */
    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var entries = Files.walk(path)) {
            entries.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
