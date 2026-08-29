package io.github.milczekt1.corral.format;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgentFriendlyFailureDisplayFormatTest {

    private static final AgentFriendlyFailureDisplayFormat FORMAT =
            new AgentFriendlyFailureDisplayFormat();

    private static final List<String> VIOLATIONS = List.of(
            "Class <com.example.OrderService> is annotated with @Transactional in (OrderService.java:12)",
            "Class <com.example.StockService> is annotated with @Transactional in (StockService.java:8)");

    private static final RuleDoc DOCUMENTED = RuleDoc.builder()
            .id("test.no-documented-rule-fixture")
            .why("This rule exists to protect invariant X.")
            .howToFix("Do Y instead.")
            .howNotToFix("Do NOT simply swap in Z — every variant is banned.")
            .build();

    private static final RuleDoc TERSE = RuleDoc.builder()
            .id("test.no-terse-rule-fixture")
            .why("Because invariant W matters.")
            .howToFix("Fix it by doing V.")
            .build();

    @Test
    void rendersEverySectionForADocumentedRule() {
        String out = FORMAT.render(DOCUMENTED, VIOLATIONS, Priority.MEDIUM);

        assertTrue(out.contains("Architecture Violation [test.no-documented-rule-fixture]"), out);
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

    /** Rendered on every failure: the excluded rule never fails, so it has no output of its own. */
    @Test
    void listsEveryExclusionInEffect() {
        List<String> census = List.of(
                "spring.no-transactional-on-final :: AspectJ load-time weaving. ADR-021.",
                "logging.no-log4j-api-directly :: the Log4j 2 API is our facade by decision.");

        String out = FORMAT.render(DOCUMENTED, VIOLATIONS, Priority.MEDIUM, census);

        assertTrue(out.contains("EXCLUDED IN THIS BUILD"), out);
        census.forEach(line -> assertTrue(out.contains(line), () -> "missing " + line + " in:\n" + out));
    }

    @Test
    void theCensusSitsBetweenTheAntiFixPolicyAndTheOffendingLocations() {
        String out = FORMAT.render(DOCUMENTED, VIOLATIONS, Priority.MEDIUM,
                List.of("logging.no-system-out :: stdout is our transport."));

        int policy = out.indexOf("HOW NOT TO FIX (always):");
        int census = out.indexOf("EXCLUDED IN THIS BUILD");
        int locations = out.indexOf("Offending locations:");

        assertTrue(policy < census && census < locations, "census out of order:\n" + out);
    }

    /** No file is the common case; an empty header would be noise on every failure. */
    @Test
    void omitsTheCensusEntirelyWhenNothingIsExcluded() {
        String out = FORMAT.render(DOCUMENTED, VIOLATIONS, Priority.MEDIUM, List.of());

        assertFalse(out.contains("EXCLUDED IN THIS BUILD"), out);
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
        // failureDisplayFormat is global, so a consumer's own rules must render unchanged. ArchUnit's
        // default is package-private and cannot be invoked here; this pins our copy of its template.
        String description = "no classes should depend on classes that reside in a package '..internal..'";

        String out = FORMAT.defaultFormat(description, VIOLATIONS, "2 times", Priority.HIGH);

        String expected = String.format(
                "Architecture Violation [Priority: HIGH] - Rule '%s' was violated (2 times):%n%s",
                description, String.join(System.lineSeparator(), VIOLATIONS));
        org.junit.jupiter.api.Assertions.assertEquals(expected, out);
        assertFalse(out.contains("HOW NOT TO FIX (always):"), "must not decorate foreign rules: " + out);
    }

    /** The never-throw contract: a throw here would replace a real violation with a stack trace. */
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

    /**
     * Drives {@link AgentFriendlyFailureDisplayFormat#formatFailure} the way ArchUnit does, through a
     * genuine {@code FailureMessages} — it has no public constructor, so ArchUnit must build it.
     */
    @BeforeEach
    void installFormatterGlobally() {
        ArchConfiguration.get().setProperty(
                "failureDisplayFormat", AgentFriendlyFailureDisplayFormat.class.getName());
    }

    @AfterEach
    void restoreArchUnitConfiguration() {
        ArchConfiguration.get().reset();
    }

    private static String renderThroughArchUnit(
            String ruleDescription, Priority priority, String... violations) {
        ConditionEvents events = ConditionEvents.Factory.create();
        for (String violation : violations) {
            events.add(new SimpleConditionEvent(new Object(), false, violation));
        }
        HasDescription rule = () -> ruleDescription;
        return new EvaluationResult(rule, events, priority).getFailureReport().toString();
    }

    @Test
    void formatFailureDecoratesADocumentedRule() {
        RuleDoc doc = RuleDoc.builder()
                .id("test.no-end-to-end-documented-fixture")
                .why("Because the invariant matters.")
                .howToFix("Do the right thing.")
                .howNotToFix("Do NOT suppress it.")
                .build();
        RuleRegistry.register(doc);

        String out = renderThroughArchUnit(doc.id(), Priority.MEDIUM,
                "Class <com.example.A> is wrong in (A.java:1)",
                "Class <com.example.B> is wrong in (B.java:2)");

        assertTrue(out.contains("Architecture Violation [test.no-end-to-end-documented-fixture]"), out);
        assertTrue(out.contains("Because the invariant matters."), out);
        assertTrue(out.contains("HOW NOT TO FIX (always):"), out);
        assertTrue(out.contains("(A.java:1)"), out);
        assertTrue(out.contains("(B.java:2)"), out);
    }

    @Test
    void formatFailureLeavesAnUnregisteredRuleUndecorated() {
        String description = "no classes should depend on '..legacy..'";

        String out = renderThroughArchUnit(description, Priority.HIGH, "Class <com.example.C> in (C.java:3)");

        assertTrue(out.contains("Rule '" + description + "' was violated"), out);
        assertTrue(out.contains("(C.java:3)"), out);
        assertFalse(out.contains("HOW NOT TO FIX (always):"), "foreign rule was decorated: " + out);
    }

    @Test
    void formatFailureReportsZeroViolationLinesWithoutFailing() {
        String out = renderThroughArchUnit("no classes should be empty-reported", Priority.LOW);

        assertTrue(out.contains("no classes should be empty-reported"), out);
    }

    @Test
    void richRenderingFallsBackToTheLastResortFormat() {
        // A null doc makes doc.id() throw inside render's try block, reaching the fallback.
        String out = FORMAT.render(null, VIOLATIONS, Priority.MEDIUM);

        assertTrue(out.contains("<unknown rule>"), out);
        assertTrue(out.contains("failed to render failure details"), out);
        assertTrue(out.contains("rich rendering threw"), out);
    }

    @Test
    void defaultRenderingFallsBackToTheLastResortFormat() {
        List<String> iterationThrows = new ArrayList<>(VIOLATIONS) {
            @Override
            public Iterator<String> iterator() {
                throw new IllegalStateException("boom");
            }
        };

        String out = FORMAT.defaultFormat("some.foreign.rule", iterationThrows, "2 times", Priority.LOW);

        assertTrue(out.contains("some.foreign.rule"), out);
        assertTrue(out.contains("default rendering threw"), out);
    }
}
