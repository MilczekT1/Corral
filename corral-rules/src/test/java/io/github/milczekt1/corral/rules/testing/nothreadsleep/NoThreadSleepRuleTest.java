package io.github.milczekt1.corral.rules.testing.nothreadsleep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class NoThreadSleepRuleTest {

    /** This rule's own examples, and nothing else — they live one package down from this test. */
    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.corral.rules.testing.nothreadsleep.fixtures");

    private static String report(ArchRule rule) {
        return String.join("\n", rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails());
    }

    @Test
    void flagsThreadSleepInATest() {
        String report = report(NoThreadSleepRule.DEFINITION);

        assertTrue(report.contains("SleepingCheckoutIT"), report);
    }

    @Test
    void flagsTimeUnitSleepInATest() {
        // TimeUnit.sleep delegates to Thread.sleep, so the rewrite must not dodge the match.
        String report = report(NoThreadSleepRule.DEFINITION);

        assertTrue(report.contains("TimeUnitSleepingIT"), report);
    }

    @Test
    void flagsASleepParkedOnATestHelperThatDeclaresNoTest() {
        String report = report(NoThreadSleepRule.DEFINITION);

        assertTrue(report.contains("SleepingTestSupport"),
                "a helper in test output is test code, which is where a pause() hides: " + report);
    }

    @Test
    void allowsWaitingOnAConditionWithACeiling() {
        String report = report(NoThreadSleepRule.DEFINITION);

        assertFalse(report.contains("PollingCheckoutIT"),
                "await(timeout) is a wait on the condition, not on the clock: " + report);
    }

    @Test
    void allowsNonSleepMethodsOnThreadAndTimeUnit() {
        // The owner alone must not decide: TimeUnit.toMillis and Thread.currentThread are not waits.
        String report = report(NoThreadSleepRule.DEFINITION);

        assertFalse(report.contains("ClockReadingIT"), report);
    }

    @Test
    void staysSilentOnTestsThatDoNotWaitAtAll() {
        String report = report(NoThreadSleepRule.DEFINITION);

        assertFalse(report.contains("WellNamedTest"), report);
        assertFalse(report.contains("PlainUnitTest"), report);
    }

    @Test
    void publicRuleIsFrozenAndIdPinned() {
        assertEquals("corral.test.no-thread-sleep", NoThreadSleepRule.rule.getDescription());
    }
}
