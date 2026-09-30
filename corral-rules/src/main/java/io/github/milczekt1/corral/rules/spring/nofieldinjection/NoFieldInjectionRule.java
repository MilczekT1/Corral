package io.github.milczekt1.corral.rules.spring.nofieldinjection;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No field of a production class may be annotated {@code @Autowired}, {@code @Inject} or
 * {@code @Resource}, in either the {@code jakarta} or the {@code javax} namespace.
 *
 * <p>Matched by annotation name, so a consumer without one of these libraries on the classpath
 * still compiles and runs the rule. One violation per field, so a fourth injected field added to a
 * class frozen with three still fails the build.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoFieldInjectionRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.no-field-injection")
            .why("""
                    A field-injected dependency is invisible from outside the class: new \
                    OrderService() compiles, runs, and throws NullPointerException at the first call, \
                    because nothing in the type says an OrderRepository was needed. Every unit test \
                    either boots a Spring context or fills the field by reflection, and a test that \
                    assembles the object by reflection breaks silently when the field is renamed. The \
                    field cannot be final, so the object is half-built between construction and \
                    injection — a constructor body sees only nulls — and nothing stops a later \
                    assignment. Spring resolves a cycle between field-injected beans without complaint, \
                    so two services that depend on each other look fine until one is tested alone. \
                    And it hides size: nine @Autowired fields read as a tidy class, where a nine-\
                    parameter constructor would show it is doing too much.""")
            .howToFix("""
                    Take the dependency as a constructor parameter, assign it to a private final field, \
                    and delete the field annotation. With exactly one constructor Spring uses it without \
                    @Autowired; Lombok's @RequiredArgsConstructor generates it from the final fields. \
                    With several constructors, put @Autowired on the one Spring should use. An optional \
                    dependency stays in the constructor too, as Optional<T>, ObjectProvider<T> or a \
                    @Nullable parameter. If the next boot fails with a circular-reference error, the \
                    cycle was already there and field injection was hiding it: extract what both sides \
                    need into a third bean, or have one side publish an event through \
                    ApplicationEventPublisher instead of calling back. Then change any test that \
                    assembled the object by reflection to pass collaborators through the constructor. \
                    A constructor too long to call comfortably in a test is the rule working: split \
                    the class.""")
            .howNotToFix("""
                    Do NOT move the annotation onto a setter — the same hidden, mutable, nullable \
                    dependency with a public mutator added. Do NOT swap @Autowired for @Inject or \
                    @Resource, or a jakarta spelling for a javax one: all five are matched. Do NOT add \
                    @Lazy to a constructor parameter to silence a circular-reference failure: it wraps \
                    a proxy around the cycle and defers the same failure to the first call. Do NOT \
                    keep the class as it is and fill the field with ReflectionTestUtils.setField or \
                    @InjectMocks in its test. Do NOT try to move the class out of scope by naming it \
                    like a test: scope is decided by output location and declared test methods. Not \
                    caught, and no less wrong: injecting ApplicationContext or BeanFactory through the \
                    constructor and pulling the dependency out with getBean, which is the service \
                    locator this rule exists to keep out; dropping the annotation and assigning a \
                    non-final field somewhere else; a project's own annotation meta-annotated with \
                    @Autowired, since only direct annotations are matched; and injection wired in XML \
                    bean definitions, which are not bytecode.""")
            .build();

    static final ArchRule DEFINITION = noFields()
            .that().areDeclaredInClassesThat(TestScope.PRODUCTION_CLASSES)
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .orShould().beAnnotatedWith("jakarta.inject.Inject")
            .orShould().beAnnotatedWith("jakarta.annotation.Resource")
            .orShould().beAnnotatedWith("javax.inject.Inject")
            .orShould().beAnnotatedWith("javax.annotation.Resource");

    @ArchTest
    public static final ArchRule rule = new NoFieldInjectionRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
