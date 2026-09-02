package io.github.milczekt1.corral.rules.testing;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Test code may not sleep: waiting for a duration is not waiting for a condition.
 *
 * <p>Scoped with {@link TestScope#TEST_CLASSES}, so a helper or abstract base in test output counts
 * even though it declares no test of its own — that is exactly where a {@code pause()} hides.
 *
 * <p>Inspects <em>test</em> classes, so consumers must not set
 * {@code ImportOption.DoNotIncludeTests} — it would pass vacuously.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoThreadSleepRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.test.no-thread-sleep")
            .why("""
                    Thread.sleep in a test encodes a guess about how long something takes, made on the \
                    machine that wrote it. On a loaded CI agent the guess is too short and the test fails \
                    for reasons unrelated to the code; on a developer laptop it is too long and the suite \
                    pays the full duration on every green run, forever. One line therefore produces both \
                    symptoms of an untrustworthy suite at once — flakes that get re-run until they pass, \
                    and a build slow enough that people stop running it locally. The usual response, \
                    doubling the sleep, makes the second symptom worse and only postpones the first, and \
                    sleeps compound: fifty of them are minutes of wall clock spent proving nothing.""")
            .howToFix("""
                    Wait for the condition, not for the clock. Poll with a timeout — Awaitility's \
                    await().atMost(...).untilAsserted(...), or the equivalent in your stack — so a fast \
                    machine finishes immediately and a slow one still passes. Better, remove the wait \
                    entirely: inject the executor so the test runs the task on the calling thread, use a \
                    synchronous test profile, or await the future the production code already returns. If \
                    the code under test offers no way to observe completion, that is the finding — a \
                    system nobody can wait on deterministically cannot be operated or tested.""")
            .howNotToFix("""
                    Do NOT wrap the call to dodge the match: TimeUnit.SECONDS.sleep(1) is matched too, and \
                    a Sleeper helper or a pause() on a base test class is matched where it is declared, \
                    whenever it compiles into test output. Relocating the wait until the rule stops seeing \
                    it is not a fix — the wait is still a guess, now harder to find. Do NOT replace it with a busy-wait loop or a CountDownLatch.await() with no \
                    timeout, which turns a flaky failure into a hung build. Do NOT paper over the flake \
                    with @RepeatedTest or a retry extension, and do NOT disable the test.""")
            .build();

    /**
     * Matched by owner and name rather than by signature, so the {@code (long)}, {@code (long, int)}
     * and {@code (Duration)} overloads are all covered without a future rewording.
     *
     * <p>{@code TimeUnit.sleep} delegates to {@code Thread.sleep}, so it is the same wait; ArchUnit
     * sees direct calls only, hence the second owner rather than a transitive walk.
     */
    private static final DescribedPredicate<JavaCall<?>> SLEEP_CALL =
            target(name("sleep"))
                    .and(target(owner(assignableTo(Thread.class)))
                            .or(target(owner(assignableTo(TimeUnit.class)))))
                    .as("target is Thread.sleep or TimeUnit.sleep");

    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.TEST_CLASSES)
            .should().callMethodWhere(SLEEP_CALL);

    @ArchTest
    public static final ArchRule rule = new NoThreadSleepRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
