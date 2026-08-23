package io.github.milczekt1.corral.scope;

import static com.tngtech.archunit.base.DescribedPredicate.describe;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

/**
 * One structural definition of "is this a test class", for every rule that needs to scope itself to
 * tests or away from them.
 *
 * <p><strong>Why not simply match the name.</strong> The obvious spelling is
 * {@code haveSimpleNameEndingWith("Test"/"Tests"/"IT")}, reusing the convention
 * {@code test.class-naming-convention} already enforces. It is unsound here, because every Corral
 * rule is independently optional <em>and</em> Corral freezes violations. A consumer with 200
 * {@code FooTestCase} classes adopts the naming rule and freezes: those 200 become recorded debt, so
 * the naming rule passes green forever while the convention it exists to guarantee is still false.
 * Every name-scoped rule then silently skips those 200 classes — green precondition, false
 * precondition, zero enforcement, nothing in the build output. The failure is not symmetric either:
 * a production-scoped rule spelled as <em>not</em> {@code *Test} would place {@code OrderTestCase}
 * on the production side and explain a {@code Thread.sleep} in a test with the pool-exhaustion
 * reasoning meant for production code — the wrong WHY delivered to the reader, which is worse than
 * silence.
 *
 * <p>Defining the scope structurally inverts the dependency. Instead of "every test-scoped rule
 * depends on the naming convention holding", it becomes "every test-scoped rule — the naming rule
 * included — depends on one structural definition of a test class".
 *
 * <p><strong>A class is a test class when either half matches, and neither half is sufficient
 * alone.</strong>
 *
 * <table border="1">
 *   <caption>What each half contributes</caption>
 *   <tr><th></th><th>Catches</th><th>Misses</th></tr>
 *   <tr>
 *     <td>Location</td>
 *     <td>fixtures, helpers, abstract bases — anything compiled into test output, whether or not it
 *         declares a test method of its own</td>
 *     <td>a build layout the three regexes do not recognise</td>
 *   </tr>
 *   <tr>
 *     <td>Structure</td>
 *     <td>test classes under any build layout and under any name</td>
 *     <td>an abstract base or helper that declares no test method of its own</td>
 *   </tr>
 * </table>
 *
 * <p>The location patterns are lifted verbatim from {@code archunit-1.4.2}'s
 * {@code ImportOption$Predefined} — the same heuristic ArchUnit already bets
 * {@code DoNotIncludeTests} on, so this adds no assumption a consumer is not already living with.
 *
 * <p><strong>Centralising the definition centralises the blast radius.</strong> Editing this class
 * changes which classes every rule scoped through it reports, so one commit can surface new
 * violations across many consumer freeze stores at once. That is the deliberate trade: the
 * alternative is the same edit repeated per rule, where the definitions drift instead.
 */
@UtilityClass
public class TestScope {

    /**
     * The three patterns ArchUnit's own {@code ImportOption.DoNotIncludeTests} matches source URIs
     * against — Maven, Gradle and IntelliJ output layouts, in that order.
     *
     * <p>Copied rather than delegated: {@code ImportOption.Predefined} exposes a predicate over
     * {@code Location}, not over an already-imported {@link JavaClass}, so there is nothing to call
     * from here. Keeping the strings identical is what makes a class scoped in as a test here the
     * same class {@code DoNotIncludeTests} would have scoped out.
     */
    private static final List<Pattern> TEST_OUTPUT_LOCATIONS = Stream.of(
                    ".*/target/test-classes/.*",            // Maven
                    ".*/build/classes/([^/]+/)?test/.*",    // Gradle
                    ".*/out/test/.*")                       // IntelliJ
            .map(Pattern::compile)
            .toList();

    /**
     * The three JUnit 5 roots, matched by FQN string so a consumer without
     * {@code junit-jupiter-params} on the classpath still compiles and runs.
     *
     * <p>Only roots are listed because the check below matches meta-annotations:
     * {@code @ParameterizedTest} and {@code @RepeatedTest} are themselves annotated
     * {@code @TestTemplate} and are reached through it, as is a consumer's own composed
     * {@code @FastTest} through {@code @Test}. Enumerating the leaves instead would mean this scope
     * silently stops recognising whatever JUnit or the consumer adds next.
     *
     * <p>Published because a rule may need to state the list rather than only apply it — pinning it
     * in a test, for instance. Immutable, so exposing it hands out no way to change what any rule
     * built on this class recognises.
     */
    public static final List<String> JUNIT_TEST_ANNOTATIONS = List.of(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.TestFactory",
            "org.junit.jupiter.api.TestTemplate");

    /**
     * Classes compiled into a recognised test output directory, or declaring at least one JUnit 5
     * test method.
     *
     * <p>The description text is part of the rendered description of any rule built on this
     * predicate, and a rule's description is its freeze-store key. Rewording it is therefore a
     * breaking change for every consumer of every rule that uses it.
     */
    public static final DescribedPredicate<JavaClass> TEST_CLASSES =
            describe("test classes", TestScope::isTestClass);

    /**
     * The exact complement of {@link #TEST_CLASSES} — every class that is not a test class.
     *
     * <p>Spelled as a negation rather than as its own predicate on purpose: two hand-written halves
     * can drift into overlapping or leaving a gap, and a class landing in both scopes would be
     * reported by a production rule and a test rule at once.
     */
    public static final DescribedPredicate<JavaClass> PRODUCTION_CLASSES =
            DescribedPredicate.not(TEST_CLASSES).as("production classes");

    private static boolean isTestClass(JavaClass clazz) {
        return isInTestOutput(clazz) || declaresTestMethod(clazz);
    }

    /**
     * Whether the class was loaded from a recognised test output directory.
     *
     * <p>{@code getSource()} is empty for a stub class — one referenced by imported code but never
     * imported itself — and an absent source counts as <em>not</em> test output, so stubs fall to
     * the production side. That is the safe default in both directions: a stub carries no method
     * bodies, so a test-scoped rule has nothing to inspect on it, and a production-scoped rule
     * reporting it would name a class the consumer may not even own.
     */
    private static boolean isInTestOutput(JavaClass clazz) {
        return clazz.getSource()
                .map(source -> source.getUri().toString())
                .filter(TestScope::matchesTestOutputLocation)
                .isPresent();
    }

    private static boolean matchesTestOutputLocation(String uri) {
        return TEST_OUTPUT_LOCATIONS.stream().anyMatch(pattern -> pattern.matcher(uri).matches());
    }

    private static boolean declaresTestMethod(JavaClass clazz) {
        return clazz.getMethods().stream().anyMatch(TestScope::isJUnitTestMethod);
    }

    /**
     * Whether a method is one JUnit 5 would execute as a test.
     *
     * <p>{@code isMetaAnnotatedWith} rather than {@code isAnnotatedWith}, and no direct check beside
     * it: ArchUnit's meta variant "also matches elements that are directly annotated with the given
     * annotation type", so a plain {@code @Test} method is covered by the same call that reaches
     * {@code @ParameterizedTest} through {@code @TestTemplate}.
     *
     * <p>Published as the seam for rules that need JUnit test <em>methods</em> without taking
     * {@link #TEST_CLASSES} wholesale. A rule scoped on {@code TEST_CLASSES} gets
     * {@code location OR structure}, which pulls in every fixture and helper compiled into test
     * output; a rule that means "this class declares a test" — {@code test.class-naming-convention}
     * is the standing example — must ask this instead, or it silently widens what it reports.
     *
     * <p>Deliberately a plain predicate method and not a {@code DescribedPredicate}: the description
     * a rule wraps this in becomes part of that rule's rendered text, which is a freeze-store
     * matching key, so each rule must own its own wording rather than inherit one from here that
     * could later be reworded on its behalf.
     */
    public static boolean isJUnitTestMethod(JavaMethod method) {
        return JUNIT_TEST_ANNOTATIONS.stream().anyMatch(method::isMetaAnnotatedWith);
    }
}
