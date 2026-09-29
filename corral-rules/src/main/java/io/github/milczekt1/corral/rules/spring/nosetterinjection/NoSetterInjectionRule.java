package io.github.milczekt1.corral.rules.spring.nosetterinjection;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No method of a production class may be annotated {@code @Autowired}, {@code @Inject} or
 * {@code @Resource}, in either the {@code jakarta} or the {@code javax} namespace.
 *
 * <p>Matched by annotation name, so a consumer without one of these libraries on the classpath
 * still compiles and runs the rule. Constructors are never matched: ArchUnit models them apart from
 * methods.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoSetterInjectionRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.no-setter-injection")
            .why("""
                    A dependency injected through a method cannot be final, so anything holding a \
                    reference can swap the collaborator at any point in the object's life. Between \
                    construction and the container's call the object is half-built: a constructor body \
                    that uses the dependency throws NullPointerException while a @PostConstruct method \
                    works, an ordering that belongs to the container and is not visible in the source. \
                    The constructor no longer states what the class needs, so new OrderService() \
                    compiles and fails at the first method call, and the public setter makes the \
                    dependency part of the class's API — a test can rebind it and leak that state into \
                    the next test. The signal a long constructor gives, that the class does too much, \
                    is gone: nine setters look like a bean.""")
            .howToFix("""
                    Delete the annotation and the method, make the field private final, and take the \
                    dependency as a constructor parameter. A class with exactly one constructor needs \
                    no @Autowired on it: Spring selects the sole constructor implicitly since 4.3. For \
                    a genuinely optional dependency, take Optional<T> or ObjectProvider<T> in the \
                    constructor rather than keeping a nullable mutable field. A circular dependency \
                    that a setter was breaking is the thing to fix: extract what both sides need into \
                    a third bean. An @Autowired multi-argument configuration method in a \
                    @Configuration class is a legitimate framework pattern; leave it frozen as debt.""")
            .howNotToFix("""
                    Do NOT move the annotation from the method onto the field — that is the same defect \
                    in a different place, and this rule does not see fields, so it would go quiet \
                    without anything being fixed. Do NOT swap @Autowired for @Inject or @Resource, in \
                    either the jakarta or the javax namespace: all five are matched. Do NOT rename the \
                    method off the set prefix: the annotation is matched, not the name. Do NOT keep \
                    the method and mark it @Deprecated while the container still calls it. Not caught: \
                    a project's own annotation meta-annotated with @Autowired — only direct \
                    annotations are matched — and injection wired in XML bean definitions, which are \
                    not bytecode.""")
            .build();

    static final ArchRule DEFINITION = noMethods()
            .that().areDeclaredInClassesThat(TestScope.PRODUCTION_CLASSES)
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .orShould().beAnnotatedWith("jakarta.inject.Inject")
            .orShould().beAnnotatedWith("jakarta.annotation.Resource")
            .orShould().beAnnotatedWith("javax.inject.Inject")
            .orShould().beAnnotatedWith("javax.annotation.Resource");

    @ArchTest
    public static final ArchRule rule = new NoSetterInjectionRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
