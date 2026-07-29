package io.github.milczekt1.archrules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FrozenRulesTest {

    @TempDir
    Path store;

    private static final RuleDoc DOC = RuleDoc.builder()
            .id("frozen.sample-rule")
            .why("Sample rule for the freeze plumbing.")
            .howToFix("Stop doing the thing.")
            .build();

    @BeforeEach
    void useTemporaryStore() {
        ArchConfiguration.get().setProperty("freeze.store.default.path", store.toString());
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreCreation", "true");
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreUpdate", "true");
    }

    @AfterEach
    void resetConfiguration() {
        ArchConfiguration.get().reset();
    }

    private static ArchRule rawRule() {
        return noClasses().that().haveSimpleNameEndingWith("Test")
                .should().haveSimpleNameStartingWith("Frozen");
    }

    @Test
    void pinsTheDescriptionToTheStableId() {
        ArchRule frozen = FrozenRules.freeze(rawRule(), DOC);

        assertEquals("frozen.sample-rule", frozen.getDescription(),
                "description IS the freeze-store key and must equal the RuleDoc id");
    }

    @Test
    void registersTheDocSoTheFormatterCanFindIt() {
        FrozenRules.freeze(rawRule(), DOC);

        assertEquals(Optional.of(DOC), RuleRegistry.find("frozen.sample-rule"));
    }

    @Test
    void writesTheFreezeStoreKeyedByTheId() throws Exception {
        ArchRule frozen = FrozenRules.freeze(rawRule(), DOC);

        frozen.check(new ClassFileImporter().importClasses(FrozenRulesTest.class));

        String storedRules = Files.readString(store.resolve("stored.rules"));
        assertTrue(storedRules.contains("frozen.sample-rule="),
                "store key must be the short id, was:\n" + storedRules);
    }

    @Test
    void firstRunSeedsExistingViolationsAndPasses() {
        ArchRule frozen = FrozenRules.freeze(rawRule(), DOC);

        // FrozenRulesTest violates rawRule(); freezing means adoption does not block.
        assertDoesNotThrow(() -> frozen.check(new ClassFileImporter().importClasses(FrozenRulesTest.class)));
    }

    @Test
    void allowsEmptyShouldSoModulesWithNoMatchingClassesStayGreen() {
        RuleDoc doc = RuleDoc.builder()
                .id("frozen.empty-should")
                .why("Nothing matches this in the test fixture.")
                .howToFix("N/A")
                .build();
        ArchRule frozen = FrozenRules.freeze(
                noClasses().that().haveSimpleNameEndingWith("NoSuchSuffixAnywhere")
                        .should().haveSimpleName("Whatever"),
                doc);

        // Without allowEmptyShould(true) this throws AssertionError about an empty should.
        assertDoesNotThrow(() -> frozen.check(new ClassFileImporter().importClasses(FrozenRulesTest.class)));
    }
}
