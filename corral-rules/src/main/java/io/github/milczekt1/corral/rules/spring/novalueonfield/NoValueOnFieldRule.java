package io.github.milczekt1.corral.rules.spring.novalueonfield;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No static or non-final field of a production class may be annotated or meta-annotated
 * {@code @Value}. Final instance fields are skipped.
 *
 * <p>Stands alone, in no group: wire it with {@code ArchTests.in(NoValueOnFieldRule.class)}. Matched
 * by annotation name, so a consumer without {@code spring-beans} still runs the rule. A
 * meta-annotation outside the imported classes, such as {@code @LocalServerPort}, is seen only while
 * {@code resolveMissingDependenciesFromClassPath} is left {@code true}. One violation per field.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoValueOnFieldRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.no-value-on-field")
            .why("""
                    A @Value field is set after the constructor has run. Until then it holds null, 0 \
                    or false, and so does every field initialiser that reads it: private final Client \
                    client = new Client(timeout) silently builds the client with no timeout. On a \
                    static field, final or not, Spring ignores @Value entirely, logging only at INFO, \
                    so the field keeps its initial value for good: null if it has none. Code that \
                    calls the constructor directly — a unit test, a new outside the container — gets \
                    an unconfigured object with no compiler complaint. Configuration is a dependency \
                    like any other, and arrives through the constructor.""")
            .howToFix("""
                    Move @Value to a constructor parameter and make the field private final. On a \
                    static field, drop static as well. A static @Value field has never received its \
                    property — the running code has always used the initialiser — so before injecting \
                    it, check what each environment sets that property to: the fix starts applying a \
                    value that was never in effect. If the initialiser is the intended value, delete \
                    the @Value instead. In a @Configuration class, a @Bean method parameter annotated @Value works too. When a \
                    class reads more than two or three settings, define a Spring Boot \
                    @ConfigurationProperties record for that prefix, register it with \
                    @ConfigurationPropertiesScan or @EnableConfigurationProperties, and inject the \
                    record instead: it gives relaxed binding and, with a Bean Validation provider, \
                    @Validated checks at startup. With Lombok's @RequiredArgsConstructor, keep the \
                    field private final and add lombok.copyableAnnotations += \
                    org.springframework.beans.factory.annotation.Value to lombok.config, so the \
                    annotation reaches the constructor parameter. The annotation stays on the field \
                    too, and Spring writes that final field again by reflection after construction: \
                    harmless while the constructor only assigns it, but from JDK 26 it logs a \
                    final-field-mutation warning (JEP 500), and a later JDK will refuse it by \
                    default. Where that matters, write the constructor by hand and remove @Value from \
                    the field. Test classes are out of scope: a class in Maven's target/test-classes, \
                    Gradle's test source-set output or IntelliJ's out/test, or one that declares a \
                    JUnit test method itself. Every other output is production — a shared \
                    test-support module's main sources and Gradle's testFixtures, integrationTest or \
                    any other source set — and an inherited test method does not count, so an \
                    abstract base test class or a @TestConfiguration there is flagged. In a base test \
                    class, take the value as a constructor parameter annotated @Value or \
                    @LocalServerPort, which Spring's JUnit extension resolves on the concrete test \
                    class and passes up.""")
            .howNotToFix("""
                    Do NOT move @Value to a setter: the same half-built object, and this rule does not \
                    catch it. Do NOT inject Environment and call env.getProperty(...) instead: the \
                    same string-keyed lookup, now hidden from the constructor's signature, and not \
                    caught either. Do NOT wrap @Value in your own annotation: a field annotation \
                    meta-annotated with @Value is matched too, as long as ArchUnit can resolve that \
                    annotation type: one outside the imported classes, such as Spring Boot's \
                    @LocalServerPort, is seen only while resolveMissingDependenciesFromClassPath is \
                    left at its default, true. Do NOT read the value from a static \
                    holder filled at startup: not caught, and global state. Do NOT leave @Value on a \
                    final field after adding it to a hand-written constructor's parameter: Spring \
                    overwrites the field after construction, undoing anything the constructor did to \
                    the value, and final instance fields are not checked, so this is not caught. Do \
                    NOT make the field final with an initialiser such as = null just to pass: not \
                    caught, Spring still writes the field after construction, and with a literal \
                    initialiser such as = 0 or = "" the class keeps reading the literal, because javac \
                    inlines it. Do NOT declare a test method on a base class to move it out of scope: \
                    a class that declares one is a test class, so this is not caught, and every \
                    subclass then runs that test. Do NOT try to move the class out of scope by naming it like a test: \
                    scope is decided by output location and declared test methods.""")
            .build();

    static final ArchRule DEFINITION = noFields()
            .that().areStatic().or().areNotFinal()
            .and().areDeclaredInClassesThat(TestScope.PRODUCTION_CLASSES)
            .should().beMetaAnnotatedWith("org.springframework.beans.factory.annotation.Value");

    @ArchTest
    public static final ArchRule rule = new NoValueOnFieldRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
