package io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures.AsyncAssertions;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures.production.TracedFuture;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures.production.UntimedFuture;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NoFutureGetWithoutTimeoutRuleTest {

    private static final String ID = "corral.concurrency.no-future-get-without-timeout";

    private static final String FUTURE_GET = "<java.util.concurrent.Future.get()>";

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = ProductionLayout.importFixtures(NoFutureGetWithoutTimeoutRuleTest.class, outputRoot.resolve("examples"));
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoFutureGetWithoutTimeoutRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String caller, String access) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains(caller + "(") && line.contains(access)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsGetOnAFutureField() {
        assertFlagged("ReportRenderer.render", "calls method " + FUTURE_GET);
    }

    @Test
    void flagsGetOnASubmittedTask() {
        assertFlagged("ReportRenderer.renderNow", "calls method " + FUTURE_GET);
    }

    @Test
    void flagsGetOnACompletableFuture() {
        assertFlagged("QuoteAggregator.blocking",
                "calls method <java.util.concurrent.CompletableFuture.get()>");
    }

    @Test
    void flagsAGetMethodReference() {
        assertFlagged("QuoteAggregator.deferred", "references method " + FUTURE_GET);
    }

    @Test
    void flagsGetInsideALambda() {
        assertFlagged("QuoteAggregator.wrapped", "calls method " + FUTURE_GET);
    }

    @Test
    void flagsGetOnAProjectsOwnFuture() {
        assertFlagged("QuoteAggregator.traced", "calls method <" + TracedFuture.class.getName() + ".get()>");
    }

    @Test
    void flagsGetOnTheDelegateOutsideAGetOverride() {
        assertFlagged("TracedFuture.result", "calls method " + FUTURE_GET);
    }

    @Test
    void flagsATimedGetOverrideWaitingOnTheUntimedGet() {
        assertFlagged("UntimedFuture.get", "calls method <" + UntimedFuture.class.getName() + ".get()>");
    }

    @Test
    void flagsGetInsideAGetOverrideOfANonFuture() {
        assertFlagged("PendingQuote.get", "calls method " + FUTURE_GET);
    }

    @Test
    void reportsExactlyNineAccesses() {
        assertEquals(9, violationsIn(examples).size(), String.join("\n", violationsIn(examples)));
    }

    @Test
    void ignoresATimedGetAndCancelBesideFlaggedCalls() {
        assertNotReported("ReportRenderer.renderWithin(");
        assertNotReported("ReportRenderer.abandon(");
    }

    @Test
    void ignoresEveryDelegationOfAProjectsOwnFuture() {
        assertNotReported("TracedFuture.get()> calls");
        assertNotReported("TracedFuture.get(long");
        assertNotReported("TracedFuture.isDone(");
        assertNotReported("TracedFuture.isCancelled(");
        assertNotReported("TracedFuture.cancel(");
    }

    @Test
    void ignoresBoundedWaitsJoinAndNoArgumentGetOnOtherTypes() {
        assertNotReported("DeadlineClient");
    }

    @Test
    void ignoresForkJoinTaskJoin() {
        assertNotReported("TileSum");
    }

    @Test
    void ignoresAGetCallInTestOutput() {
        assertNotReported("AsyncAssertions");
    }

    /** The same class file as above, moved: only its location changed, so only scope can explain the difference. */
    @Test
    void flagsTheTestOutputAsyncAssertionsOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("assertions"),
                List.of(AsyncAssertions.class));
        JavaClasses assertions = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(assertions);

        assertTrue(violations.stream().anyMatch(line -> line.contains("AsyncAssertions.awaited(")),
                String.join("\n", violations));
    }

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(NoFutureGetWithoutTimeoutRuleTest.class, NoFutureGetWithoutTimeoutRule.DEFINITION, examples);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoFutureGetWithoutTimeoutRule.rule, ID, examples);

        assertTrue(debt.contains("ReportRenderer.render("), debt);
        assertTrue(debt.contains("ReportRenderer.renderNow("), debt);
        assertTrue(debt.contains("QuoteAggregator.blocking("), debt);
        assertTrue(debt.contains("QuoteAggregator.deferred("), debt);
        assertTrue(debt.contains("QuoteAggregator.wrapped("), debt);
        assertTrue(debt.contains("QuoteAggregator.traced("), debt);
        assertTrue(debt.contains("TracedFuture.result("), debt);
        assertTrue(debt.contains("UntimedFuture.get(long"), debt);
        assertTrue(debt.contains("PendingQuote.get("), debt);
    }
}
