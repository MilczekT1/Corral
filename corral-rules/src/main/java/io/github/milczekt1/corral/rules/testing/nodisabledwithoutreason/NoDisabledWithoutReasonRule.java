package io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * {@code @Disabled} must carry a reason, on the class or on the method that declares it.
 *
 * <p>Matches <em>direct</em> annotations only. A project's own {@code @DisabledPendingFix}, itself
 * annotated {@code @Disabled}, is flagged where it is <em>declared</em> — the one place a reason can
 * be written once for every use — and never where it is used. Reaching the {@code @Disabled}
 * instance through a meta-annotation has no ArchUnit API, and walking {@code getAnnotations()} by
 * hand is import-scope dependent: the meta-annotation graph has self-loops ({@code @Retention} is
 * itself {@code @Retention}), and whether the walk terminates or overflows the stack depends on
 * whether the consumer imported {@code java.lang.annotation}. A false negative costs coverage; that
 * walk costs consumers their build.
 *
 * <p>Conditional variants — {@code @DisabledOnOs}, {@code @DisabledIfEnvironmentVariable} and the
 * rest — are separate annotation types and are deliberately never matched: they state their
 * condition and still run everywhere else.
 *
 * <p>Inspects <em>test</em> classes, so consumers must not set
 * {@code ImportOption.DoNotIncludeTests} — it would pass vacuously.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoDisabledWithoutReasonRule implements DocumentedRule {

    /** Matched by FQN string, so nothing here needs {@code junit-jupiter-api} on the compile path. */
    static final String DISABLED = "org.junit.jupiter.api.Disabled";

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.test.junit.no-disabled-without-reason")
            .why("""
                    A disabled test is reported as skipped once, in a build log nobody reads, and is then \
                    permanently invisible. Nothing in the build ever forces it to be re-evaluated: the code \
                    it covers keeps changing underneath it, so by the time somebody tries to re-enable it, \
                    it fails for reasons that have nothing to do with why it was disabled, and gets \
                    disabled again. Meanwhile the behaviour it was written to protect ships untested while \
                    the file sitting in the source tree says otherwise. The reason string is what makes \
                    that recoverable: "flaky under parallel execution, see #412" is a decision somebody can \
                    act on — check the issue, confirm it is closed, delete the annotation. A bare @Disabled \
                    is a dead end, because whoever wrote it has moved on, nobody knows whether the test is \
                    wrong or the code is, and the only safe move is to leave it alone. JUnit makes the \
                    reason optional, so the bare form is what an IDE completes and what a hurried commit \
                    leaves behind.""")
            .howToFix("""
                    Best: fix the test or delete it, which are the only two outcomes that restore coverage. \
                    If neither is available right now, write the reason — what is blocking it, and what has \
                    to happen before it can come back. Name an issue if there is one. If the test is \
                    disabled because it is flaky, the flake is a bug in the test or in the code under test, \
                    most often a sleep-based wait standing in for a real synchronisation point, and fixing \
                    that is what re-enables the test. If it should only run in some environments, that is \
                    not a disable at all: use a conditional annotation — @DisabledOnOs, \
                    @DisabledIfEnvironmentVariable, @EnabledIf... — which states the condition, still runs \
                    everywhere else, and is not matched here. If the violation names an annotation type \
                    rather than a test, put the reason on that annotation's own @Disabled: every use of it \
                    then carries the reason.""")
            .howNotToFix("""
                    Do NOT write a reason that says nothing — "flaky", "TODO", "broken". This rule cannot \
                    tell those from a real one and will pass them; it is only worth having if the reason is \
                    written for the person who finds it in a year. Do NOT gut the method body and drop the \
                    annotation so an empty test passes: an empty test method is green, counted in the \
                    report next to real tests, and asserts nothing, so that trades a hole the build reports \
                    as skipped for one it reports as passing. Do NOT rename the class to something the \
                    build tool does not select, such as OrderServiceTestDisabled — Surefire and Failsafe \
                    pick classes by name, so that leaves the same dead test with the alarm removed. Do NOT \
                    swap @Disabled for JUnit 4's @Ignore: the Jupiter engine does not recognise JUnit 4 \
                    annotations at all, so the method is discovered as not being a test and is not even \
                    reported as skipped. Two dodges this rule genuinely does not catch, and which are just \
                    as dead either way: moving the test behind a @Tag the Surefire configuration excludes, \
                    and hiding the bare @Disabled inside your own composed annotation, whose uses are not \
                    matched. Do NOT add the new violation to the freeze store by hand; the store records \
                    what existed at adoption, not what you just wrote.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.TEST_CLASSES)
            .should(declareADisabledWithoutAReason());

    @ArchTest
    public static final ArchRule rule = new NoDisabledWithoutReasonRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }

    /**
     * Used with {@code noClasses().should(...)}, so a satisfied event is reported as a violation.
     *
     * <p>One event per offending element rather than one per class: a class and its methods are one
     * failure mode under one freeze-store key, but a fourth unexplained disable added to a class
     * whose first three are frozen debt must still fail the build.
     */
    private static ArchCondition<JavaClass> declareADisabledWithoutAReason() {
        return new ArchCondition<>("declare @Disabled without a reason") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (isDisabledWithoutAReason(javaClass)) {
                    events.add(SimpleConditionEvent.satisfied(javaClass,
                            "Class " + javaClass.getFullName() + " is @Disabled with no reason"));
                }
                for (JavaMethod method : javaClass.getMethods()) {
                    if (isDisabledWithoutAReason(method)) {
                        events.add(SimpleConditionEvent.satisfied(method,
                                "Method " + method.getFullName() + " is @Disabled with no reason"));
                    }
                }
            }
        };
    }

    /**
     * {@code get} falls back to the annotation type's default, so the bare form yields {@code ""} and
     * this one check covers it, {@code @Disabled("")} and {@code @Disabled("   ")} alike.
     */
    private static boolean isDisabledWithoutAReason(HasAnnotations<?> element) {
        return element.tryGetAnnotationOfType(DISABLED)
                .map(disabled -> String.valueOf(disabled.get("value").orElse("")))
                .filter(String::isBlank)
                .isPresent();
    }
}
