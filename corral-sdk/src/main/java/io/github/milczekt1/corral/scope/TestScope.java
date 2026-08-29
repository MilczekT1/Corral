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
 * One structural definition of "is this a test class", for every rule that scopes itself to tests or
 * away from them. A class qualifies on <strong>either</strong> test output location or a declared
 * JUnit test method — never on its name, which a consumer can freeze away.
 *
 * <p>Editing this changes what every rule scoped through it reports, across every consumer's store.
 */
@UtilityClass
public class TestScope {

    /**
     * The patterns {@code ImportOption.DoNotIncludeTests} matches source URIs against. Copied, not
     * delegated: {@code ImportOption.Predefined} takes a {@code Location}, not a {@link JavaClass}.
     */
    private static final List<Pattern> TEST_OUTPUT_LOCATIONS = Stream.of(
                    ".*/target/test-classes/.*",            // Maven
                    ".*/build/classes/([^/]+/)?test/.*",    // Gradle
                    ".*/out/test/.*")                       // IntelliJ
            .map(Pattern::compile)
            .toList();

    /**
     * The three JUnit 5 roots, matched by FQN string so a consumer without
     * {@code junit-jupiter-params} still compiles. Roots only — {@link #isJUnitTestMethod} matches
     * meta-annotations, so {@code @ParameterizedTest} and a consumer's own {@code @FastTest} follow.
     */
    public static final List<String> JUNIT_TEST_ANNOTATIONS = List.of(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.TestFactory",
            "org.junit.jupiter.api.TestTemplate");

    /**
     * Classes in a recognised test output directory, or declaring a JUnit 5 test method.
     *
     * <p>The description text renders into every rule built on it, which is that rule's freeze-store
     * key — rewording it re-seeds every consumer's store.
     */
    public static final DescribedPredicate<JavaClass> TEST_CLASSES =
            describe("test classes", TestScope::isTestClass);

    /** The exact complement of {@link #TEST_CLASSES}. */
    public static final DescribedPredicate<JavaClass> PRODUCTION_CLASSES =
            DescribedPredicate.not(TEST_CLASSES).as("production classes");

    private static boolean isTestClass(JavaClass clazz) {
        return isInTestOutput(clazz) || declaresTestMethod(clazz);
    }

    /** {@code getSource()} is empty for a stub class, so stubs fall to the production side. */
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
     * Whether JUnit 5 would execute this method as a test. {@code isMetaAnnotatedWith} also matches a
     * directly annotated element, so this covers {@code @Test} and {@code @ParameterizedTest} alike.
     *
     * <p>The seam for rules meaning "declares a test"; {@link #TEST_CLASSES} also matches fixtures and
     * helpers in test output. Not a {@code DescribedPredicate} — each rule owns its own wording.
     */
    public static boolean isJUnitTestMethod(JavaMethod method) {
        return JUNIT_TEST_ANNOTATIONS.stream().anyMatch(method::isMetaAnnotatedWith);
    }
}
