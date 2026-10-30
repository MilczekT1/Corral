package io.github.milczekt1.corral.settings;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static io.github.milczekt1.corral.settings.CorralSettings.BASE_PACKAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import com.tngtech.archunit.library.freeze.ViolationStore;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.fixtures.settings.service.PrintingService;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class BasePackageCoverageTest {

    private static final RuleDoc REQUIRING_BASE_PACKAGE = RuleDoc.builder()
            .id("test.no-base-package-coverage-fixture")
            .why("A fixture for BasePackageCoverageTest.")
            .howToFix("N/A — this is a test fixture, not a real rule.")
            .requiredSetting(BASE_PACKAGE)
            .build();

    private static final JavaClasses PRINTING_SERVICE = new ClassFileImporter().importClasses(PrintingService.class);

    @Test
    void aBasePackageMatchingNoImportedClassFailsTheRule() {
        ArchRule covered = BasePackage.requireMatchingClasses(printlnInServices("com.elsewhere"),
                REQUIRING_BASE_PACKAGE, () -> basePackage("com.elsewhere"));

        AssertionError thrown = assertThrows(AssertionError.class, () -> covered.evaluate(PRINTING_SERVICE));
        assertThrows(AssertionError.class, () -> covered.check(PRINTING_SERVICE));

        assertTrue(thrown.getMessage().contains(BASE_PACKAGE), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("com.elsewhere"), thrown.getMessage());
    }

    @Test
    void aBasePackageMatchingAnImportedClassEvaluatesTheRule() {
        String base = "io.github.milczekt1.corral.fixtures";
        ArchRule covered = BasePackage.requireMatchingClasses(printlnInServices(base),
                REQUIRING_BASE_PACKAGE, () -> basePackage(base));

        assertTrue(covered.evaluate(PRINTING_SERVICE).hasViolation());
    }

    @Test
    void aRuleNotDeclaringTheBasePackageIsUnchanged() {
        RuleDoc declaringNothing = RuleDoc.builder()
                .id("test.no-undeclared-coverage-fixture")
                .why("A fixture for BasePackageCoverageTest.")
                .howToFix("N/A — this is a test fixture, not a real rule.")
                .build();
        ArchRule rule = noClasses().should().accessField(System.class, "in");

        assertSame(rule, BasePackage.requireMatchingClasses(rule, declaringNothing, SettingsFile::none));
    }

    @Test
    void chainingKeepsTheCheck() {
        ArchRule covered = BasePackage.requireMatchingClasses(printlnInServices("com.elsewhere"),
                REQUIRING_BASE_PACKAGE, () -> basePackage("com.elsewhere"));

        ArchRule chained = covered.as("renamed").because("reasons").allowEmptyShould(true);

        assertEquals("renamed, because reasons", chained.getDescription());
        assertThrows(AssertionError.class, () -> chained.evaluate(PRINTING_SERVICE));
    }

    /** Inside the freeze, an unmatched base package must fail before the store sees an empty result. */
    @Test
    void anUnmatchedBasePackageNeverReachesTheStore() {
        RecordingStore store = new RecordingStore();
        ArchRule frozen = FreezingArchRule.freeze(BasePackage.requireMatchingClasses(
                        printlnInServices("com.elsewhere"), REQUIRING_BASE_PACKAGE, () -> basePackage("com.elsewhere")))
                .persistIn(store);

        assertThrows(AssertionError.class, () -> frozen.check(PRINTING_SERVICE));
        assertTrue(store.saved.isEmpty(), () -> "the store was written: " + store.saved);
    }

    private static ArchRule printlnInServices(String base) {
        return noClasses()
                .that(BasePackage.resideIn(REQUIRING_BASE_PACKAGE, "..service..", () -> basePackage(base)))
                .should().callMethod(PrintStream.class, "println", String.class)
                .allowEmptyShould(true);
    }

    private static SettingsFile basePackage(String value) {
        return SettingsFile.parse(BASE_PACKAGE + "=" + value, "file:/somewhere/" + CorralSettings.FILE);
    }

    private static final class RecordingStore implements ViolationStore {

        private final List<String> saved = new ArrayList<>();

        @Override
        public void initialize(Properties properties) {
            // Nothing to configure.
        }

        @Override
        public boolean contains(ArchRule rule) {
            return true;
        }

        @Override
        public void save(ArchRule rule, List<String> violations) {
            saved.add(rule.getDescription() + " " + violations);
        }

        @Override
        public List<String> getViolations(ArchRule rule) {
            return List.of("Method <io.github.milczekt1.corral.fixtures.settings.service.PrintingService.announce"
                    + "(java.lang.String)> calls method <java.io.PrintStream.println(java.lang.String)>");
        }
    }
}
