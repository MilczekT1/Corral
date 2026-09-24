package io.github.milczekt1.corral.rules.testing.nomutablestaticstate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A test class must not declare a {@code static} non-{@code final} field.
 *
 * <p>Stands alone, in no group: wire it with {@code ArchTests.in(NoMutableStaticStateRule.class)},
 * the same way a group names it.
 *
 * <p>{@code @TempDir} is exempt because JUnit assigns that field, so it cannot be {@code final}.
 * {@code @RegisterExtension} and Testcontainers' {@code @Container} can both be {@code static final}
 * and are not exempt.
 *
 * <p>Reads modifiers only: nothing but the {@code @TempDir} exemption is JUnit-specific, and
 * {@link TestScope} decides what counts as a test class, never a naming convention. Compiler-
 * generated fields are not excluded, so a JVM language that emits static non-final fields of its own
 * is flagged on them.
 *
 * <p>Inspects <em>test</em> classes, so consumers must not set
 * {@code ImportOption.DoNotIncludeTests} — it would pass vacuously.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoMutableStaticStateRule implements DocumentedRule {

    /** Matched by FQN string, so nothing here needs {@code junit-jupiter-api} on the compile path. */
    static final String TEMP_DIR = "org.junit.jupiter.api.io.TempDir";

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.test.no-mutable-static-state")
            .why("""
                    JUnit constructs a fresh instance of the test class for every test method, \
                    precisely so that one test cannot influence another. A static field opts out of \
                    that: it is initialised once per class load and carries whatever the previous test \
                    wrote into it, across methods and across classes sharing the JVM fork. What that \
                    produces is order dependence — the suite passes with the whole class selected and \
                    fails when one method is run from the IDE, or the reverse — and the order is \
                    JUnit's own, deterministic but unspecified, so renaming a method reshuffles it. \
                    Turn on parallel execution and the same field is a data race, and the suite starts \
                    failing at random on the machine with more cores. All three present as flakiness, \
                    and none of them points at the field.""")
            .howToFix("""
                    Make it an instance field. That is usually the entire fix: the per-method instance \
                    already gives you the fresh state the static was working around, and a @BeforeEach \
                    populates it. If the value is genuinely shared and expensive to build — a parsed \
                    schema, a compiled template — make it static final and keep what it refers to \
                    immutable. If it is a counter or an accumulator carried between methods, the tests \
                    depend on each other: inline the setup into each test, even at the cost of \
                    repetition, because a test that only passes after its neighbour ran is not a test. \
                    A @TempDir field is exempt and needs no change — JUnit assigns it, so it cannot be \
                    final.""")
            .howNotToFix("""
                    Do NOT make the field static final while leaving the object it refers to mutable — \
                    static final List<Order> ORDERS = new ArrayList<>(). This rule stops matching and \
                    every problem above remains; it is the most likely wrong fix here and the one \
                    place this rule's sight ends, so nothing will tell you. Do NOT keep the field \
                    static and clear it in an @AfterEach: that is isolation by hand, it stops working \
                    the moment a test returns early or fails before the cleanup runs, and it does \
                    nothing at all for parallel execution. Do NOT stabilise the order instead of \
                    removing the sharing — @Execution(SAME_THREAD) and @TestMethodOrder both freeze the \
                    symptom in place, and @TestInstance(PER_CLASS) only makes the sharing look \
                    deliberate. Do NOT add the new violation to the freeze store by hand; the store \
                    records what existed at adoption, not what you just wrote.""")
            .build();

    static final ArchRule DEFINITION = noFields()
            .that().areDeclaredInClassesThat(TestScope.TEST_CLASSES)
            .and().areNotAnnotatedWith(TEMP_DIR)
            .and().areNotFinal()
            .should().beStatic();

    @ArchTest
    public static final ArchRule rule = new NoMutableStaticStateRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
