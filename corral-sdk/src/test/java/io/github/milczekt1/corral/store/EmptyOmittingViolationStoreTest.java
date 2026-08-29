package io.github.milczekt1.corral.store;

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
 * Drives {@link EmptyOmittingViolationStore} directly, on a local {@link Properties} instance, so
 * there is no global {@code ArchConfiguration} to reset. The exception is
 * {@link #freezingArchRuleFailsOnAViolationIntroducedAfterACleanFreeze()}, which goes through
 * {@code FreezingArchRule} and resets it in a {@code finally} block.
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
        // The store is global, so it also serves rules frozen without a RuleDoc — whole sentences.
        ArchRule documented = ruleNamed("test.documented-rule");
        ArchRule foreign = ruleNamed("no classes should depend on classes that reside in '..internal..'");

        store.save(documented, List.of("Class <Foo> is bad"));
        store.save(foreign, List.of("Class <Bar> is bad"));

        assertTrue(Files.exists(storeDir.resolve("test.documented-rule")));
        assertEquals(List.of("Class <Bar> is bad"), store.getViolations(foreign),
                "a rule named after prose must still round-trip through its generated file name");

        // Pin the shape of the fallback: a round-trip alone would pass on a sanitized file name too.
        String generated = indexEntryFor(foreign.getDescription());
        assertNotNull(generated, "a prose-named rule must still get an index entry");
        assertTrue(generated.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"),
                "a prose description must map to a UUID, not to the description: " + generated);
        assertTrue(Files.exists(storeDir.resolve(generated)),
                "the index must point at a file that exists: " + generated);
    }

    @Test
    void anOverlongIdShapedDescriptionFallsBackToAUuid() throws Exception {
        // A rule frozen without a RuleDoc can carry a dot/kebab-shaped description that exceeds the
        // caps, so fileNameFor must apply them itself.
        String tooLong = "test.documented-rule-" + "a".repeat(60);
        ArchRule overlong = ruleNamed(tooLong);

        store.save(overlong, List.of("Class <Foo> is bad"));

        assertTrue(Files.notExists(storeDir.resolve(tooLong)),
                "an overlong id-shaped description must not reach the filesystem as a file name");
        String generated = indexEntryFor(overlong.getDescription());
        assertTrue(generated.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"),
                "an overlong id-shaped description must map to a UUID: " + generated);
    }

    @Test
    void aRuleFrozenCleanIsStillContainedSoItsFirstViolationFails() {
        // No file must not mean "unknown rule": FreezingArchRule seeds-and-passes what it lacks.
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

            // Stock ArchUnit writes an empty file here, so the file count is what tests the decorator.
            assertEquals(0, countViolationFiles(),
                    "a clean freeze through FreezingArchRule must leave no violation file");

            assertThrows(AssertionError.class, () -> rule.check(dirty),
                    "a violation appearing after a clean freeze must FAIL, not be seeded as debt");
        } finally {
            ArchConfiguration.get().reset();
        }
    }


    @Test
    void theIndexIsWrittenInSortedOrderWhateverTheRunningJdkDoes() throws Exception {
        // Properties.store() sorts by key only since JDK 21; on 17-20 it writes hash-bucket order.
        List<String> ids = List.of(
                "spring.no-field-injection", "api.no-array-return-types", "test.no-thread-sleep",
                "logging.no-system-out", "jakarta.no-final-entity", "naming.no-impl-suffix",
                "concurrency.no-thread-sleep", "security.no-runtime-exec", "java.no-legacy-date-api",
                "lombok.no-builder-with-setters", "jackson.no-default-typing", "layering.no-package-cycles",
                "exception.no-error-subclass", "corral.exclusions-must-name-real-rules");
        for (String id : ids) {
            store.save(ruleNamed(id), List.of("Class <Foo> violates " + id));
        }

        List<String> keys = Files.readAllLines(storeDir.resolve("stored.rules")).stream()
                .filter(line -> !line.startsWith("#"))
                .map(line -> line.split("=")[0])
                .toList();

        assertEquals(keys.stream().sorted().toList(), keys,
                "stored.rules must be sorted so a consumer's committed store yields a reviewable diff");
    }

    /** Counts rule violation files only, excluding the {@code stored.rules} index itself. */
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
