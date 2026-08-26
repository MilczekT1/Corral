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
 * <p><strong>Not the class name.</strong> Every Corral rule is independently optional and Corral
 * freezes violations, so a consumer can freeze
 * {@code test.class-names-must-end-with-test-or-it}'s violations as debt: it passes green forever
 * while the convention it guarantees stays false, silently unarming every name-scoped rule.
 *
 * <p>A class is a test class when <strong>either</strong> half matches; neither is sufficient alone.
 * Location catches fixtures, helpers and abstract bases that declare no test method; structure
 * catches tests under a build layout the patterns do not recognise.
 *
 * <p>Editing this class changes what every rule scoped through it reports, so one commit can
 * surface new violations across many consumer freeze stores at once.
 */
@UtilityClass
public class TestScope {

    /**
     * The patterns ArchUnit's own {@code ImportOption.DoNotIncludeTests} matches source URIs
     * against. Copied, not delegated: {@code ImportOption.Predefined} exposes a predicate over
     * {@code Location}, not over an imported {@link JavaClass}.
     */
    private static final List<Pattern> TEST_OUTPUT_LOCATIONS = Stream.of(
                    ".*/target/test-classes/.*",            // Maven
                    ".*/build/classes/([^/]+/)?test/.*",    // Gradle
                    ".*/out/test/.*")                       // IntelliJ
            .map(Pattern::compile)
            .toList();

    /**
     * The three JUnit 5 roots, matched by FQN string so a consumer without
     * {@code junit-jupiter-params} still compiles.
     *
     * <p>Only roots, because the check below matches meta-annotations: {@code @ParameterizedTest}
     * and {@code @RepeatedTest} are reached through {@code @TestTemplate}, and a consumer's own
     * {@code @FastTest} through {@code @Test}. Listing the leaves would stop recognising whatever
     * JUnit adds next. Immutable, so publishing it hands out no way to change it.
     */
    public static final List<String> JUNIT_TEST_ANNOTATIONS = List.of(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.TestFactory",
            "org.junit.jupiter.api.TestTemplate");

    /**
     * Classes in a recognised test output directory, or declaring a JUnit 5 test method.
     *
     * <p>This description text renders into any rule built on it, and a rule's description is its
     * freeze-store key. Rewording it breaks every consumer of every such rule.
     */
    public static final DescribedPredicate<JavaClass> TEST_CLASSES =
            describe("test classes", TestScope::isTestClass);

    /**
     * The exact complement of {@link #TEST_CLASSES}. A negation rather than a second hand-written
     * predicate, so the two halves cannot drift into overlapping or leaving a gap.
     */
    public static final DescribedPredicate<JavaClass> PRODUCTION_CLASSES =
            DescribedPredicate.not(TEST_CLASSES).as("production classes");

    private static boolean isTestClass(JavaClass clazz) {
        return isInTestOutput(clazz) || declaresTestMethod(clazz);
    }

    /**
     * {@code getSource()} is empty for a stub class — referenced but never imported — so stubs fall
     * to the production side. Safe either way: a stub has no method bodies to inspect.
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
     * Whether JUnit 5 would execute this method as a test. {@code isMetaAnnotatedWith} also matches
     * a directly annotated element, so one call covers plain {@code @Test} and
     * {@code @ParameterizedTest} alike.
     *
     * <p>The seam for rules meaning "this class declares a test" — {@link #TEST_CLASSES} is
     * {@code location OR structure} and would also pull in every fixture and helper in test output.
     *
     * <p>Deliberately not a {@code DescribedPredicate}: the wording a rule wraps this in becomes
     * that rule's freeze-store key, so each rule must own it.
     */
    public static boolean isJUnitTestMethod(JavaMethod method) {
        return JUNIT_TEST_ANNOTATIONS.stream().anyMatch(method::isMetaAnnotatedWith);
    }
}
