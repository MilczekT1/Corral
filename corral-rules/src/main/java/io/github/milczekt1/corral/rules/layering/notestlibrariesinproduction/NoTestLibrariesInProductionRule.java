package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No production class may depend on a test framework, mocking, assertion or test-container library.
 *
 * <p>Stands alone, in no group: wire it with {@code ArchTests.in(NoTestLibrariesInProductionRule.class)}.
 * A module whose product is test support — a JUnit extension, a Testcontainers module, a rule catalog
 * — depends on these libraries from its main sources by design and should not wire it.
 *
 * <p>Test code is decided by output location alone, unlike {@code TestScope}: Gradle
 * {@code testFixtures} output and any source set named {@code *test} or {@code *Test} count as test
 * code, and a class declaring a JUnit test method in production output is checked.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoTestLibrariesInProductionRule implements DocumentedRule {

    /**
     * Do not widen the Spring entries to {@code org.springframework..test..}: production jars carry
     * such packages ({@code amqp.utils.test}, {@code cloud.function.context.test}).
     */
    private static final String[] TEST_LIBRARY_PACKAGES = {
            "org.junit..", "junit..", "org.opentest4j..", "org.testng..",
            "spock..", "org.spockframework..", "io.kotest..",
            "org.mockito..", "org.powermock..", "mockit..", "org.easymock..", "io.mockk..",
            "org.assertj..", "org.hamcrest..", "com.google.common.truth..",
            "org.testcontainers..",
            "org.springframework.test..", "org.springframework.mock..", "org.springframework.*.test..",
            "org.springframework.boot..test..", "org.springframework.boot.resttestclient..",
            "org.springframework.boot.webtestclient..", "reactor.test..",
            "org.awaitility..", "io.restassured.."};

    /**
     * {@code TestScope}'s patterns, plus Gradle {@code testFixtures} and {@code *test}/{@code *Test}
     * source sets. The optional segment after {@code kotlin/} is a Kotlin Multiplatform target
     * ({@code kotlin/jvm/test}). Do not allow it after another language or let it be {@code main}:
     * a top-level package such as {@code latest} would then read as a test source set.
     */
    private static final List<Pattern> TEST_OUTPUT_LOCATIONS = Stream.of(
                    ".*/target/test-classes/.*",
                    ".*/build/classes/([^/]+/)?test/.*",
                    ".*/out/test/.*",
                    ".*/build/classes/(?:kotlin/(?:(?!main/)[^/]+/)?|[^/]+/)(?:testFixtures|[^/]*[tT]est)/.*")
            .map(Pattern::compile)
            .toList();

    private static final DescribedPredicate<JavaClass> PRODUCTION_SCOPE =
            describe("production classes", clazz -> !isInTestOutput(clazz));

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.layering.no-test-libraries-in-production")
            .why("""
                    Test libraries belong on the test classpath. Maven's test scope and Gradle's \
                    testImplementation keep them off the main compile classpath, so a production class \
                    can only reference one when the dependency was declared at compile or \
                    implementation scope by mistake, here or in a module this one depends on — a \
                    missing <scope>test</scope> on spring-boot-starter-test is the classic case. After \
                    that, IDE auto-import pulls in look-alike helpers such as \
                    org.assertj.core.util.VisibleForTesting, and the scope can no longer be fixed \
                    without breaking the build. The test framework then ships in the deployed artifact \
                    and, for a library, leaks onto every consumer's runtime classpath, where it collides \
                    with their own test-library versions. An assertion used as an argument check \
                    throws an AssertionError subclass, which catch (Exception e) does not catch. \
                    Covered: JUnit, opentest4j, TestNG, Spock, Kotest, Mockito, PowerMock, JMockit, \
                    EasyMock, MockK, AssertJ, Hamcrest, Truth, Testcontainers, Awaitility, REST \
                    Assured, spring-test (org.springframework.test and org.springframework.mock), \
                    Spring's org.springframework.<project>.test packages, Spring Boot's test modules \
                    (org.springframework.boot..test.., resttestclient, webtestclient), and \
                    reactor-test. WireMock, MockWebServer, XMLUnit, JSONAssert, JsonPath and \
                    ArchUnit are not banned: each has legitimate production use.""")
            .howToFix("""
                    Decide which side the class belongs on. Test support — a builder, a fixture \
                    factory, a custom assertion — moves to src/test/java, a Gradle testFixtures source \
                    set, or a test-support module that does not wire this rule; a test class compiled \
                    into production output is checked like any other class, @Test methods or not. \
                    Production code that reached for a test library for convenience calls a \
                    production API instead: \
                    Objects.requireNonNull, an IllegalArgumentException, Spring's \
                    org.springframework.util.Assert or Guava's Preconditions. An annotation borrowed \
                    from a test library, such as AssertJ's @VisibleForTesting, is deleted or replaced \
                    with Guava's com.google.common.annotations.VisibleForTesting. A Spring mock used as \
                    a real object — MockMultipartFile wrapping a file, MockHttpServletRequest for \
                    rendering outside a request — becomes a production implementation of the \
                    interface, or the API takes bytes or a Resource instead. A collaborator that only \
                    exists in tests becomes an interface in production code, implemented in the test. \
                    Then set the dependency back to test scope; if it arrives through another \
                    dependency, exclude it there or fix that module's scope. A module whose product is \
                    test support — a JUnit extension, a Testcontainers module, a shared assertion \
                    library — should not wire this rule at all.""")
            .howNotToFix("""
                    Do NOT leave the dependency at compile scope once the class is fixed: the framework \
                    still ships. This rule cannot see dependency scope, so it passes either way. Do NOT \
                    replace the call with a hand-written helper that throws AssertionError; throw \
                    NullPointerException or IllegalArgumentException. This rule does not catch it, \
                    because java.lang.AssertionError is not a test library. Do NOT switch to another \
                    test library's assertions or mocks: this rule flags only the libraries it lists, \
                    so an unlisted one passes but ships the same way. Do NOT move the class into a \
                    package the ArchUnit import excludes, or into a Gradle source set named like a \
                    test set when it is shipped code: this rule does not catch either.""")
            .build();

    static final ArchRule DEFINITION = noClasses().that(PRODUCTION_SCOPE)
            .should().dependOnClassesThat().resideInAnyPackage(TEST_LIBRARY_PACKAGES);

    @ArchTest
    public static final ArchRule rule = new NoTestLibrariesInProductionRule().guard();

    /** {@code getSource()} is empty for a stub class, so stubs fall to the production side. */
    private static boolean isInTestOutput(JavaClass clazz) {
        return clazz.getSource()
                .map(source -> isTestOutput(source.getUri().toString()))
                .orElse(false);
    }

    static boolean isTestOutput(String uri) {
        return TEST_OUTPUT_LOCATIONS.stream().anyMatch(pattern -> pattern.matcher(uri).matches());
    }

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
