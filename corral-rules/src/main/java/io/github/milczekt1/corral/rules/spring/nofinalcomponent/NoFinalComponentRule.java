package io.github.milczekt1.corral.rules.spring.nofinalcomponent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No production class meta-annotated {@code @Component} — directly, or through {@code @Service},
 * {@code @Repository}, {@code @Controller}, {@code @Configuration} or a project's own composed
 * annotation — may be {@code final}. Records count, being implicitly final.
 *
 * <p>Matched by annotation name, so a consumer without {@code spring-context} still runs the rule.
 * One violation per class.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoFinalComponentRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.no-final-component")
            .why("""
                    Spring Boot proxies beans with CGLIB by default, and a CGLIB proxy is a runtime \
                    subclass of the bean. A final class cannot have one, so the moment anything advises \
                    the bean — @Transactional, @Async, @Cacheable, @PreAuthorize, @Validated, \
                    @Retryable, a scoped proxy, or an @Aspect whose pointcut happens to match — \
                    creating it fails with "Cannot subclass final class". Nobody has to touch the final \
                    class: someone enables method security or writes an aspect over a whole package, \
                    and a bean in another module breaks. The failure is loud but routinely escapes the \
                    test suite, because it only happens where the real bean is created and advised: \
                    tests that mock the bean or load only slices never build it, the profile carrying \
                    @EnableCaching or @EnableMethodSecurity is often off in tests, and under lazy \
                    initialization the context starts, health checks pass, and the first request that \
                    reaches the bean fails in production.""")
            .howToFix("""
                    Remove the final modifier from the class. A Spring-managed bean is a type the \
                    container may need to subclass, so final is a promise it cannot keep; if the intent \
                    was "do not extend this", say so in the class Javadoc. If the type genuinely must \
                    stay final — a value type, a record — it should not be a bean: remove the \
                    stereotype annotation and construct it where it is used, or pass it in as a plain \
                    collaborator.""")
            .howNotToFix("""
                    Do NOT set spring.aop.proxy-target-class=false to get JDK dynamic proxies: it is \
                    application-wide, works only for beans that implement an interface, breaks every \
                    injection point that declares an advised bean by its concrete class — and this \
                    check still flags the class. Do NOT remove the @Transactional, @Cacheable or other \
                    advice that first exposed the problem. Do NOT try to move the class out of scope by \
                    naming it like a test: scope is decided by output location and declared test \
                    methods. Not caught, and no less broken: making the class sealed instead of final, \
                    which the JVM refuses to let CGLIB subclass just the same; and removing the \
                    stereotype while still registering the final class through a @Bean method, which \
                    leaves it a bean an aspect can match.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.PRODUCTION_CLASSES)
            .and().areMetaAnnotatedWith("org.springframework.stereotype.Component")
            .should().haveModifier(JavaModifier.FINAL);

    @ArchTest
    public static final ArchRule rule = new NoFinalComponentRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
