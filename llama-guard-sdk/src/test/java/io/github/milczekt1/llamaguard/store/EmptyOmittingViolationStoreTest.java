package io.github.milczekt1.llamaguard.store;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives {@link EmptyOmittingViolationStore} directly rather than through {@code FreezingArchRule},
 * so each behaviour is isolated. Most of these tests do not touch {@code ArchConfiguration}'s global
 * properties: the store under test is constructed and initialized with a local {@link Properties}
 * instance, so there is nothing global to reset in {@code @AfterEach}. The exception is
 * {@link #freezingArchRuleFailsOnAViolationIntroducedAfterACleanFreeze()}, which drives the decorator
 * through {@code FreezingArchRule} itself and therefore must configure and reset
 * {@code ArchConfiguration} on its own, in a {@code finally} block.
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
    void aRuleIdBecomesTheFileNameWhileProseKeepsAUuid() throws Exception {
        // The store is global, so it also serves rules frozen without a RuleDoc. Their descriptions
        // are whole sentences and must not reach the filesystem as file names.
        ArchRule documented = ruleNamed("test.documented-rule");
        ArchRule foreign = ruleNamed("no classes should depend on classes that reside in '..internal..'");

        store.save(documented, List.of("Class <Foo> is bad"));
        store.save(foreign, List.of("Class <Bar> is bad"));

        assertTrue(Files.exists(storeDir.resolve("test.documented-rule")));
        assertEquals(List.of("Class <Bar> is bad"), store.getViolations(foreign),
                "a rule named after prose must still round-trip through its generated file name");

        // Round-trip alone would also pass if prose were sanitized into a file name, which is the
        // failure this store exists to prevent — spaces and quotes reaching the filesystem. Pin the
        // shape of the fallback, not just that reading it back works.
        String generated = indexEntryFor(foreign.getDescription());
        assertNotNull(generated, "a prose-named rule must still get an index entry");
        assertTrue(generated.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"),
                "a prose description must map to a UUID, not to the description: " + generated);
        assertTrue(Files.exists(storeDir.resolve(generated)),
                "the index must point at a file that exists: " + generated);
    }

    @Test
    void aRuleFrozenCleanIsStillContainedSoItsFirstViolationFails() {
        // No file must not mean "unknown rule". FreezingArchRule seeds-and-passes anything the store
        // does not contain, so if this ever returns false a clean rule's first real violation would
        // be absorbed as debt and the build would stay green.
        ArchRule rule = ruleNamed("test.clean-then-dirty");
        store.save(rule, List.of());

        assertTrue(store.contains(rule),
                "a clean rule with no file must still be contained");
        assertEquals(List.of(), store.getViolations(rule));
    }

    @Test
    void freezingArchRuleFailsOnAViolationIntroducedAfterACleanFreeze() throws IOException {
        ArchConfiguration.get().setProperty("freeze.store.default.path", storeDir.toString());
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreCreation", "true");
        ArchConfiguration.get().setProperty("freeze.store", EmptyOmittingViolationStore.class.getName());
        try {
            JavaClasses clean = new ClassFileImporter().importClasses(String.class);
            JavaClasses dirty = new ClassFileImporter().importClasses(String.class, Integer.class);

            ArchRule rule = FreezingArchRule.freeze(
                    noClasses().that().haveSimpleName("Integer")
                            .should().haveSimpleName("Integer")
                            .as("test.freeze-roundtrip").allowEmptyShould(true));

            rule.check(clean);   // seeds clean: entry written, no file

            // Without this the test would pass identically against stock ArchUnit, which writes an
            // empty file here. Asserting the file count is what makes it a test of the decorator.
            assertEquals(0, countViolationFiles(),
                    "a clean freeze through FreezingArchRule must leave no violation file");

            assertThrows(AssertionError.class, () -> rule.check(dirty),
                    "a violation appearing after a clean freeze must FAIL, not be seeded as debt");
        } finally {
            ArchConfiguration.get().reset();
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

    /** The delegate's {@code <rule-description>=<file-name>} index, read the way the store reads it. */
    private String indexEntryFor(String ruleDescription) throws IOException {
        Properties index = new Properties();
        try (var in = Files.newInputStream(storeDir.resolve("stored.rules"))) {
            index.load(in);
        }
        return index.getProperty(ruleDescription);
    }
}
