package io.github.milczekt1.corral.settings;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static io.github.milczekt1.corral.settings.CorralSettings.BASE_PACKAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequiredSettingsTest {

    private static final String SOURCE = "file:/somewhere/" + CorralSettings.FILE;

    private static final ArchRule RULE = noClasses().should().accessField(System.class, "in")
            .as("test.no-required-settings-fixture");

    private static final JavaClasses SOME_CLASSES = new ClassFileImporter().importClasses(RequiredSettingsTest.class);

    @Test
    void aRuleWithAllSettingsIsUnchanged() {
        assertSame(RULE, RequiredSettings.applyTo(RULE, requiring(BASE_PACKAGE), parse(BASE_PACKAGE + "=com.a")));
    }

    @Test
    void aMissingSettingFailsTheRuleNamingKeyAndFile() {
        ArchRule applied = RequiredSettings.applyTo(RULE, requiring(BASE_PACKAGE), SettingsFile.none());

        AssertionError thrown = assertThrows(AssertionError.class, () -> applied.check(SOME_CLASSES));
        assertThrows(AssertionError.class, () -> applied.evaluate(SOME_CLASSES));

        assertTrue(thrown.getMessage().contains(BASE_PACKAGE), thrown.getMessage());
        assertTrue(thrown.getMessage().contains(CorralSettings.FILE), thrown.getMessage());
        assertEquals(RULE.getDescription(), applied.getDescription());
    }

    @Test
    void aMissingSettingFailsEvenWhenNothingMatches() {
        ArchRule matchesNothing = noClasses().that().resideInAPackage("nothing.here")
                .should().accessField(System.class, "in")
                .allowEmptyShould(true);

        ArchRule applied = RequiredSettings.applyTo(matchesNothing, requiring(BASE_PACKAGE), SettingsFile.none());

        assertThrows(AssertionError.class, () -> applied.check(new ClassFileImporter().importClasses()));
    }

    @Test
    void everyMissingSettingIsListed() {
        ArchRule applied = RequiredSettings.applyTo(RULE, requiring(BASE_PACKAGE, "acme.x"), SettingsFile.none());

        AssertionError thrown = assertThrows(AssertionError.class, () -> applied.check(SOME_CLASSES));

        assertTrue(thrown.getMessage().contains(BASE_PACKAGE), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("acme.x"), thrown.getMessage());
    }

    @Test
    void aBlankConsumerSettingCountsAsMissing() {
        ArchRule applied = RequiredSettings.applyTo(RULE, requiring("acme.x"), parse("acme.x=   "));

        assertThrows(AssertionError.class, () -> applied.check(SOME_CLASSES));
    }

    @Test
    void aBrokenFileLeavesTheRuleToTheExclusionPath() {
        assertSame(RULE, RequiredSettings.applyTo(RULE, requiring(BASE_PACKAGE), SettingsFile.broken("boom")));
    }

    @Test
    void anUndeclaredConsumerKeyWarns() {
        String warning = RequiredSettings.undeclaredKeysWarning(parse("acme.servce-package=x"), List.of(requiring()));

        assertTrue(warning.contains("acme.servce-package"), warning);
        assertTrue(warning.contains(CorralSettings.FILE), warning);
    }

    @Test
    void aDeclaredConsumerKeyDoesNotWarn() {
        assertNull(RequiredSettings.undeclaredKeysWarning(
                parse("acme.service-package=x"), List.of(requiring("acme.service-package"))));
    }

    @Test
    void corralKeysNeverWarn() {
        assertNull(RequiredSettings.undeclaredKeysWarning(
                parse(BASE_PACKAGE + "=com.a\ncorral.exclude.a.no-b=why"), List.of(requiring())));
    }

    private static RuleDoc requiring(String... keys) {
        RuleDoc.RuleDocBuilder builder = RuleDoc.builder()
                .id("test.no-required-settings-fixture")
                .why("A fixture for RequiredSettingsTest.")
                .howToFix("N/A — this is a test fixture, not a real rule.");
        for (String key : keys) {
            builder.requiredSetting(key);
        }
        return builder.build();
    }

    private static SettingsFile parse(String contents) {
        return SettingsFile.parse(contents, SOURCE);
    }
}
