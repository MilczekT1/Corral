package io.github.milczekt1.corral.rules.testing.nojunit4;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class compiled into test output may depend on anything {@code junit:junit} ships.
 *
 * <p>Scoped to test <em>output</em>, not to classes declaring a test: the shared base class with a
 * JUnit 4 {@code @Before} and no tests of its own is the class the fixture never runs for, and the
 * only place it can be caught.
 *
 * <p>Inspects <em>test</em> classes, so consumers must not set
 * {@code ImportOption.DoNotIncludeTests} — it would pass vacuously.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoJUnit4Rule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.test.no-junit4")
            .why("""
                    A class annotated org.junit.Test compiles whenever junit:junit is on the test \
                    classpath — usually transitively, from a dependency nobody chose — and the Jupiter \
                    engine does not recognise it. The class is discovered as containing no tests, so the \
                    build is green with none of its assertions ever evaluated: nothing is printed, nothing \
                    is reported as skipped, and one package segment separates org.junit.Test from \
                    org.junit.jupiter.api.Test. A half-migrated file is worse. Jupiter's @Test beside \
                    JUnit 4's @Before runs the tests but never the setup, so they execute against null \
                    fields; and JUnit 4's assertEquals takes the message first where Jupiter's takes it \
                    last, so switching the static import without touching the call sites still compiles \
                    and now compares the message to the expected value. The vector is rarely the test \
                    class. It is the abstract base class, the @Rule on a helper, the fixture builder \
                    still importing org.junit.Assert — code with no test methods of its own that every \
                    test in the package extends or calls.""")
            .howToFix("""
                    The violation names the JUnit 4 type it found, and there are two kinds. If it names \
                    an annotation or a runner, migrate the member: org.junit.Test to \
                    org.junit.jupiter.api.Test, @Before/@After to @BeforeEach/@AfterEach, \
                    @BeforeClass/@AfterClass to @BeforeAll/@AfterAll, @RunWith(MockitoJUnitRunner.class) \
                    to @ExtendWith(MockitoExtension.class), @Rule/@ClassRule to the extension the library \
                    ships for JUnit 5, and delete the test rather than carrying an @Ignore. That part is \
                    mechanical and the compiler catches what you miss. If it names Assert, Assume or \
                    TestCase, it is not: switch to org.junit.jupiter.api.Assertions AND move the message \
                    argument to the end of every call you touch, because the compiler will not tell you \
                    if you forget. For anything richer than equality, and for every Hamcrest assertThat, \
                    move to AssertJ. Then remove junit:junit as a declared test dependency, or find the \
                    parent that brings it in with mvn dependency:tree — leaving it on the classpath is \
                    what lets the mixed state survive compilation.""")
            .howNotToFix("""
                    Do NOT add junit-vintage-engine so the JUnit 4 annotations run again: that buys a \
                    green build by keeping two engines, two annotation vocabularies and two lifecycle \
                    models alive in one suite, which is the state this rule exists to end. Do NOT change \
                    only the import and leave the argument order — that is the bug half this rule exists \
                    to stop, and it produces a green build. Do NOT wrap the JUnit 4 assertions in a \
                    project helper so the call target is no longer org.junit.Assert: the argument-order \
                    hazard survives the wrapper and is now invisible, and the helper is what this rule \
                    flags instead. Do NOT swap @Test for @Ignore or delete the annotation so the method \
                    quietly becomes dead code, do NOT delete assertions to make a migrated test compile, \
                    and do NOT leave assertTrue(true) behind as a placeholder — nothing else catches that \
                    one. One dodge this predicate does NOT catch: re-exporting org.junit.Test as a \
                    meta-annotation on your own @FastTest. dependOnClassesThat is not transitive, so it \
                    flags FastTest itself and goes quiet on every class using it. That is a weak dodge \
                    rather than a working one — JUnit 4 does not honour meta-annotations at runtime, so \
                    those tests still do not execute — but do not read the silence as a fix. Do NOT \
                    hand-edit the freeze store to admit a new entry: it records debt you inherited, not \
                    debt you just wrote.""")
            .build();

    /**
     * The subtrees of {@code org.junit..} that JUnit 4 does not own. Each is load-bearing and a real
     * package: {@code org.junit.jupiter..} is JUnit 5's own API, {@code org.junit.platform..} is
     * legitimately imported ({@code @Suite}, {@code Testable}), and {@code org.junit.vintage..} is the
     * engine a mid-migration consumer configures on purpose — configuring it is not a violation, only
     * using JUnit 4's API is.
     */
    static final List<String> NOT_JUNIT4_PACKAGES = List.of(
            "org.junit.jupiter..",
            "org.junit.platform..",
            "org.junit.vintage..");

    /**
     * Everything {@code junit:junit:4.13.2} ships, matched by package string so a consumer without the
     * artifact still compiles and runs the rule.
     */
    private static final DescribedPredicate<JavaClass> JUNIT4_TYPES =
            resideInAnyPackage("junit.framework..", "junit.extensions..")
                    .or(resideInAPackage("org.junit..")
                            .and(not(resideInAnyPackage(NOT_JUNIT4_PACKAGES.toArray(String[]::new)))))
                    .as("a JUnit 4 or JUnit 3 type");

    /**
     * One dependency check covers the whole surface: annotations on classes, methods and fields, field
     * and parameter types, superclasses ({@code extends junit.framework.TestCase}) and method calls
     * ({@code Assert.assertEquals}). Violations are therefore per-dependency, not per-class — a class
     * with {@code @Before}, {@code @Test} and an {@code Assert} call freezes as three entries, so
     * adding a fourth JUnit 4 usage to an already-frozen class is a new violation and is blocked.
     */
    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.TEST_CLASSES)
            .should().dependOnClassesThat(JUNIT4_TYPES);

    @ArchTest
    public static final ArchRule rule = new NoJUnit4Rule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
