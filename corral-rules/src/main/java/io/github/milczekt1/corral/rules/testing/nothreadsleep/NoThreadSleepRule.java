package io.github.milczekt1.corral.rules.testing.nothreadsleep;

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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Test code may not call {@code Thread.sleep}.
 *
 * <p>{@code TimeUnit.sleep} and other spellings of a wait are out of scope: same mistake, different
 * predicate, and an id each, so a consumer can adopt and retire them separately.
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
                    Do NOT move the sleep out of this rule's sight. A Sleeper helper or a pause() on a base \
                    test class is matched where it is declared, whenever it compiles into test output, and \
                    relocating the wait until the rule goes quiet leaves it exactly as much of a guess, now \
                    harder to find. This rule matches Thread.sleep and nothing else, so what dodges it is \
                    still wrong and now unenforced: the same duration spelled through another API is the \
                    same guess, a busy-wait loop or an await() or Future.get() with no timeout trades a \
                    flaky failure for a hung build, and @RepeatedTest or a retry extension averages the \
                    flake out rather than removing it. Green here means no Thread.sleep was found, not that \
                    the test waits on anything.""")
            .build();

    /** Owner and name, not signature: every overload, without a rewording that would re-seed stores. */
    private static final DescribedPredicate<JavaCall<?>> SLEEP_CALL =
            target(owner(assignableTo(Thread.class)))
                    .and(target(name("sleep")))
                    .as("target is Thread.sleep");

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
