package io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures.AsyncAssertions;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures.DeadlineClient;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures.PendingQuote;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures.QuoteAggregator;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures.ReportRenderer;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures.ThrowingSupplier;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures.TileSum;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures.TracedFuture;
import io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures.UntimedFuture;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The rule is scoped to production classes, and every fixture compiles into test output. So the
 * examples are copied into a Gradle main-output layout and imported from there;
 * {@link AsyncAssertions} alone is imported from where it compiled.
 */
class NoFutureGetWithoutTimeoutRuleTest {

    private static final String ID = "corral.concurrency.no-future-get-without-timeout";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final String FUTURE_GET = "<java.util.concurrent.Future.get()>";

    private static final List<Class<?>> PRODUCTION_EXAMPLES = List.of(
            ReportRenderer.class, QuoteAggregator.class, TracedFuture.class, DeadlineClient.class,
            ThrowingSupplier.class, TileSum.class, UntimedFuture.class, PendingQuote.class);

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("examples"), PRODUCTION_EXAMPLES);
        examples = new ClassFileImporter().importLocations(
                ProductionLayout.withTestOutput(productionOutput, AsyncAssertions.class));

        assertEquals(PRODUCTION_EXAMPLES.size() + 1, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
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

    /**
     * {@link FreezingArchRule#persistIn}, not {@code freeze.store}: a frozen rule captures its store
     * when constructed, which is class initialisation, so naming one here races class loading. Only
     * the path goes on the process-wide {@link ArchConfiguration}.
     *
     * <p>Reseed with {@code -Darchunit.freeze.store.default.allowStoreCreation=true}, then commit.
     */
    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        ArchConfiguration.get().setProperty("freeze.store.default.path", STORE_PATH);
        try {
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoFutureGetWithoutTimeoutRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("ReportRenderer.render("), debt);
            assertTrue(debt.contains("ReportRenderer.renderNow("), debt);
            assertTrue(debt.contains("QuoteAggregator.blocking("), debt);
            assertTrue(debt.contains("QuoteAggregator.deferred("), debt);
            assertTrue(debt.contains("QuoteAggregator.wrapped("), debt);
            assertTrue(debt.contains("QuoteAggregator.traced("), debt);
            assertTrue(debt.contains("TracedFuture.result("), debt);
            assertTrue(debt.contains("UntimedFuture.get(long"), debt);
            assertTrue(debt.contains("PendingQuote.get("), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
