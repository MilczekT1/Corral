package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No production class may depend on a type assignable to Spring's
 * {@code org.springframework.beans.factory.BeanFactory} — {@code ApplicationContext},
 * {@code ListableBeanFactory}, {@code ConfigurableApplicationContext} and every other subtype.
 *
 * <p>Matched by name, so the class loads without Spring on the classpath. It needs ArchUnit's default
 * {@code resolveMissingDependenciesFromClassPath=true}: with it off, {@code ApplicationContext} and its
 * subtypes are stubs with no supertypes, and only a literal {@code BeanFactory} dependency is reported.
 * One violation per dependency.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoApplicationContextInjectionRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.no-application-context-injection")
            .why("""
                    A class that holds the ApplicationContext and calls getBean(...) is a service \
                    locator. Its real dependencies are no longer in the constructor, so nobody can see \
                    what it uses. Wiring mistakes move from startup to runtime: a missing or ambiguous \
                    bean no longer stops the context from starting, it throws on the code path that \
                    first asks for it, in production, possibly months later. Tests then need a real \
                    context, or a mocked container stubbed per lookup. ApplicationContextAware and \
                    BeanFactoryAware are the same thing, delivered through a callback.""")
            .howToFix("""
                    Take the collaborator as a constructor parameter. To choose among several at \
                    runtime, inject Map<String, Strategy> (keyed by bean name) or List<Strategy> and \
                    select from it. For a lazy, optional or prototype-scoped dependency, inject \
                    ObjectProvider<Strategy> and call getObject(). To publish events, inject \
                    ApplicationEventPublisher — it is not matched. A BeanFactoryPostProcessor class or an \
                    ApplicationContextInitializer, whose signature hands over the container, is \
                    reported too, and so is a Condition or a Quartz JobFactory that calls the bean \
                    factory: leave it as frozen debt, so the store lists exactly where the container is \
                    touched.""")
            .howNotToFix("""
                    Do NOT inject a narrower container type — ListableBeanFactory, \
                    ConfigurableApplicationContext, AutowireCapableBeanFactory: every subtype of \
                    BeanFactory is matched. Do NOT move the lookup into a static holder \
                    (SpringContextHolder.getBean(...)): the holder class is reported, but its callers \
                    are not, so calls through an existing, frozen holder pass the build and are the \
                    same coupling plus global state; a holder shipped by a library is not reported \
                    anywhere. Do NOT implement ApplicationContextAware on a base \
                    class and inherit it: the base class is reported, and so is a subclass calling \
                    getApplicationContext().getBean(...), but a subclass calling a lookup helper the \
                    base class defines, such as bean(Strategy.class), is not, and is still wrong. Do \
                    NOT try to move the class out of scope by naming it like a test: scope is decided \
                    by output location and declared test methods.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.PRODUCTION_CLASSES)
            .should().dependOnClassesThat().areAssignableTo("org.springframework.beans.factory.BeanFactory");

    @ArchTest
    public static final ArchRule rule = new NoApplicationContextInjectionRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
