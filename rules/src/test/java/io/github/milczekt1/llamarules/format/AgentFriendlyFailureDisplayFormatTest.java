package io.github.milczekt1.llamarules.format;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.lang.Priority;
import io.github.milczekt1.llamarules.doc.RuleDoc;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentFriendlyFailureDisplayFormatTest {

    private static final AgentFriendlyFailureDisplayFormat FORMAT =
            new AgentFriendlyFailureDisplayFormat();

    private static final List<String> VIOLATIONS = List.of(
            "Class <com.example.OrderService> is annotated with @Transactional in (OrderService.java:12)",
            "Class <com.example.StockService> is annotated with @Transactional in (StockService.java:8)");

    private static final RuleDoc DOCUMENTED = RuleDoc.builder()
            .id("format.documented-rule")
            .why("This rule exists to protect invariant X.")
            .howToFix("Do Y instead.")
            .howNotToFix("Do NOT simply swap in Z — every variant is banned.")
            .build();

    private static final RuleDoc TERSE = RuleDoc.builder()
            .id("format.terse-rule")
            .why("Because invariant W matters.")
            .howToFix("Fix it by doing V.")
            .build();

    @Test
    void rendersEverySectionForADocumentedRule() {
        String out = FORMAT.render(DOCUMENTED, VIOLATIONS, Priority.MEDIUM);

        assertTrue(out.contains("Architecture Violation [format.documented-rule]"), out);
        assertTrue(out.contains("WHY:"), out);
        assertTrue(out.contains("This rule exists to protect invariant X."), out);
        assertTrue(out.contains("HOW TO FIX:"), out);
        assertTrue(out.contains("Do Y instead."), out);
        assertTrue(out.contains("HOW NOT TO FIX (this rule):"), out);
        assertTrue(out.contains("Do NOT simply swap in Z"), out);
        assertTrue(out.contains("HOW NOT TO FIX (always):"), out);
        assertTrue(out.contains("Offending locations:"), out);
    }

    @Test
    void sectionsAppearInTeachingOrder() {
        String out = FORMAT.render(DOCUMENTED, VIOLATIONS, Priority.MEDIUM);

        int why = out.indexOf("WHY:");
        int fix = out.indexOf("HOW TO FIX:");
        int perRule = out.indexOf("HOW NOT TO FIX (this rule):");
        int global = out.indexOf("HOW NOT TO FIX (always):");
        int locations = out.indexOf("Offending locations:");

        assertTrue(why < fix && fix < perRule && perRule < global && global < locations,
                "sections out of order:\n" + out);
    }

    @Test
    void alwaysRendersTheFullGlobalAntiFixPolicy() {
        String out = FORMAT.render(DOCUMENTED, VIOLATIONS, Priority.MEDIUM);

        for (String clause : AntiFixPolicy.clauses()) {
            assertTrue(out.contains(clause), "missing anti-fix clause: " + clause);
        }
    }

    @Test
    void omitsThePerRuleSectionWhenTheDocHasNoHowNotToFix() {
        String out = FORMAT.render(TERSE, VIOLATIONS, Priority.MEDIUM);

        assertFalse(out.contains("HOW NOT TO FIX (this rule):"), out);
        assertTrue(out.contains("HOW NOT TO FIX (always):"), "global policy is never optional: " + out);
    }

    @Test
    void includesEveryViolationLine() {
        String out = FORMAT.render(DOCUMENTED, VIOLATIONS, Priority.MEDIUM);

        for (String violation : VIOLATIONS) {
            assertTrue(out.contains(violation), "missing violation line: " + violation);
        }
    }

    @Test
    void defaultFormatLeavesForeignRulesUndecorated() {
        // failureDisplayFormat is global: a consumer's own rules pass through this formatter too,
        // and must come out looking exactly as they would without the framework installed.
        // ArchUnit's own default lives in a package-private class and cannot be invoked here, so
        // this pins our copy of its template — matching the real one is verified by reading it.
        String description = "no classes should depend on classes that reside in a package '..internal..'";

        String out = FORMAT.defaultFormat(description, VIOLATIONS, "2 times", Priority.HIGH);

        String expected = String.format(
                "Architecture Violation [Priority: HIGH] - Rule '%s' was violated (2 times):%n%s",
                description, String.join(System.lineSeparator(), VIOLATIONS));
        org.junit.jupiter.api.Assertions.assertEquals(expected, out);
        assertFalse(out.contains("HOW NOT TO FIX (always):"), "must not decorate foreign rules: " + out);
    }

    /**
     * One consolidated check for the never-throw contract. The formatter runs inside a failing
     * build, so throwing here would replace a real architecture violation with a stack trace.
     */
    @Test
    void neverThrowsOnHostileOrNullInput() {
        HasDescription hostile = () -> {
            throw new IllegalStateException("boom");
        };
        List<String> withNullElement = new ArrayList<>(VIOLATIONS);
        withNullElement.add(null);

        assertTrue(AgentFriendlyFailureDisplayFormat.describeSafely(hostile).contains("unknown rule"));
        assertTrue(AgentFriendlyFailureDisplayFormat.describeSafely(null).contains("unknown rule"));

        assertDoesNotThrow(() -> FORMAT.render(DOCUMENTED, null, Priority.MEDIUM));
        assertDoesNotThrow(() -> FORMAT.render(DOCUMENTED, VIOLATIONS, null));
        assertDoesNotThrow(() -> FORMAT.render(DOCUMENTED, withNullElement, Priority.MEDIUM));

        assertDoesNotThrow(() -> FORMAT.defaultFormat("some.rule", null, "2 times", Priority.HIGH));
        assertDoesNotThrow(() -> FORMAT.defaultFormat("some.rule", VIOLATIONS, "2 times", null));

        assertDoesNotThrow(() -> FORMAT.lastResortFormat(hostile, null, null));
        assertDoesNotThrow(() -> FORMAT.lastResortFormat(null, null, null));
    }
}
