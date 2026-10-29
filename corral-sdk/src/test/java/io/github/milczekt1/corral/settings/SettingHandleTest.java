package io.github.milczekt1.corral.settings;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static io.github.milczekt1.corral.settings.CorralSettings.BASE_PACKAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.fixtures.settings.service.PrintingService;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SettingHandleTest {

    private static final RuleDoc REQUIRING_BASE_PACKAGE = RuleDoc.builder()
            .id("test.no-setting-handle-fixture")
            .why("A fixture for SettingHandleTest.")
            .howToFix("N/A — this is a test fixture, not a real rule.")
            .requiredSetting(BASE_PACKAGE)
            .build();

    private static final JavaClasses PRINTING_SERVICE = new ClassFileImporter().importClasses(PrintingService.class);

    @Test
    void readingAnUndeclaredKeyFailsAtOnce() {
        RuleDoc declaringNothing = RuleDoc.builder()
                .id("test.no-undeclared-setting-fixture")
                .why("A fixture for SettingHandleTest.")
                .howToFix("N/A — this is a test fixture, not a real rule.")
                .build();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> CorralSettings.setting(declaringNothing, BASE_PACKAGE));

        assertTrue(thrown.getMessage().contains(BASE_PACKAGE), thrown.getMessage());
    }

    @Test
    void theValueIsResolvedOnGetNotOnCreation() {
        AtomicInteger reads = new AtomicInteger();
        Supplier<SettingsFile> source = () -> {
            reads.incrementAndGet();
            return basePackage("com.acme");
        };

        Supplier<String> handle = CorralSettings.setting(REQUIRING_BASE_PACKAGE, BASE_PACKAGE, source);
        assertEquals(0, reads.get());

        assertEquals("com.acme", handle.get());
        assertEquals(1, reads.get());
    }

    @Test
    void aMissingValueFailsOnGetNamingKeyAndFile() {
        Supplier<String> handle = CorralSettings.setting(REQUIRING_BASE_PACKAGE, BASE_PACKAGE, SettingsFile::none);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, handle::get);

        assertTrue(thrown.getMessage().contains(BASE_PACKAGE), thrown.getMessage());
        assertTrue(thrown.getMessage().contains(CorralSettings.FILE), thrown.getMessage());
    }

    @Test
    void basePackagePredicateDescriptionHidesTheValue() {
        String underA = BasePackage.resideIn(REQUIRING_BASE_PACKAGE, "..service..", () -> basePackage("com.a"))
                .getDescription();
        String underB = BasePackage.resideIn(REQUIRING_BASE_PACKAGE, "..service..", () -> basePackage("com.b"))
                .getDescription();

        assertEquals(underA, underB);
        assertFalse(underA.contains("com.a"), underA);
    }

    @Test
    void violationTextIsIdenticalUnderTwoBasePackages() {
        EvaluationResult narrow = printlnInServices("io.github.milczekt1.corral.fixtures.settings")
                .evaluate(PRINTING_SERVICE);
        EvaluationResult wide = printlnInServices("io.github.milczekt1.corral.fixtures").evaluate(PRINTING_SERVICE);

        assertTrue(narrow.hasViolation());
        assertEquals(1, narrow.getFailureReport().getDetails().size(),
                () -> "print(String) must not be flagged: " + narrow.getFailureReport().getDetails());
        assertEquals(narrow.getFailureReport().getDetails(), wide.getFailureReport().getDetails());
        assertEquals(printlnInServices("com.a").getDescription(), printlnInServices("com.b").getDescription());
    }

    @Test
    void basePackagePredicateDoesNotMatchOutsideTheBase() {
        assertFalse(printlnInServices("com.elsewhere").evaluate(PRINTING_SERVICE).hasViolation());
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
}
