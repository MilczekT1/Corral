// Lives in the `groups` package on purpose: it reads DatabaseRules' package-private
// raw-rule and doc constants.
package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.archrules.FrozenRules;
import io.github.milczekt1.archrules.format.AntiFixPolicy;
import io.github.milczekt1.archrules.fixtures.database.service.AnnotatedService;
import io.github.milczekt1.archrules.fixtures.database.service.CleanService;
import io.github.milczekt1.archrules.fixtures.database.service.SecondAnnotatedService;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FreezingBehaviourTest {

    @TempDir
    Path store;

    private static final JavaClasses PRE_EXISTING =
            new ClassFileImporter().importClasses(AnnotatedService.class, CleanService.class);

    private static final JavaClasses WITH_NEW_VIOLATION = new ClassFileImporter()
            .importClasses(AnnotatedService.class, CleanService.class, SecondAnnotatedService.class);

    @BeforeEach
    void useTemporaryStoreAndTheAgentFriendlyFormatter() {
        ArchConfiguration.get().setProperty("freeze.store.default.path", store.toString());
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreCreation", "true");
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreUpdate", "true");
        ArchConfiguration.get().setProperty("failureDisplayFormat",
                "io.github.milczekt1.archrules.format.AgentFriendlyFailureDisplayFormat");
    }

    @AfterEach
    void resetConfiguration() {
        ArchConfiguration.get().reset();
    }

    /** A fresh frozen instance per call; the shared temp store is what carries state. */
    private static ArchRule frozenRule() {
        return FrozenRules.freeze(DatabaseRules.NO_TX_ON_CLASSES_RULE, DatabaseRules.NO_TX_ON_CLASSES_DOC);
    }

    @Test
    void adoptionDoesNotBlock_firstRunSeedsExistingViolationsAndPasses() {
        assertDoesNotThrow(() -> frozenRule().check(PRE_EXISTING));
    }

    @Test
    void onlyNewViolationsFail() {
        frozenRule().check(PRE_EXISTING); // seed

        AssertionError failure = assertThrows(AssertionError.class,
                () -> frozenRule().check(WITH_NEW_VIOLATION));

        assertTrue(failure.getMessage().contains("SecondAnnotatedService"),
                "the new violation must be reported: " + failure.getMessage());
        // NB: ".service.AnnotatedService>" — a bare "AnnotatedService>" is also a substring of
        // "SecondAnnotatedService>", so the package prefix is what makes this discriminate.
        assertFalse(failure.getMessage().contains(".service.AnnotatedService>"),
                "the frozen pre-existing violation must stay silent: " + failure.getMessage());
    }

    @Test
    void reRunningWithoutNewViolationsStaysGreen() {
        frozenRule().check(PRE_EXISTING);

        assertDoesNotThrow(() -> frozenRule().check(PRE_EXISTING));
    }

    @Test
    void theFailureTeachesWhyAndHowToFix() {
        frozenRule().check(PRE_EXISTING);

        AssertionError failure = assertThrows(AssertionError.class,
                () -> frozenRule().check(WITH_NEW_VIOLATION));
        String message = failure.getMessage();

        assertTrue(message.contains("Architecture Violation [db.no-spring-transactional-on-classes]"), message);
        assertTrue(message.contains("WHY:"), message);
        assertTrue(message.contains(DatabaseRules.NO_TX_ON_CLASSES_DOC.why()), message);
        assertTrue(message.contains("HOW TO FIX:"), message);
        assertTrue(message.contains(DatabaseRules.NO_TX_ON_CLASSES_DOC.howToFix()), message);
        assertTrue(message.contains("HOW NOT TO FIX (this rule):"), message);
        assertTrue(message.contains("Offending locations:"), message);
    }

    @Test
    void theFailureCarriesTheWholeGlobalAntiFixPolicy() {
        frozenRule().check(PRE_EXISTING);

        AssertionError failure = assertThrows(AssertionError.class,
                () -> frozenRule().check(WITH_NEW_VIOLATION));

        assertTrue(failure.getMessage().contains("HOW NOT TO FIX (always):"), failure.getMessage());
        for (String clause : AntiFixPolicy.clauses()) {
            assertTrue(failure.getMessage().contains(clause), "missing clause: " + clause);
        }
    }
}
