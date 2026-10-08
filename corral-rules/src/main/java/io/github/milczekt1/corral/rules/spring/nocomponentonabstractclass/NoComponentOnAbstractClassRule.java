package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No abstract production class may be meta-annotated {@code @Component} — directly, or through
 * {@code @Service}, {@code @Repository}, {@code @Controller} or a project's own composed annotation.
 * Abstract {@code @Configuration} classes, interfaces, and abstract classes declaring a
 * {@code @Lookup} method are not matched.
 *
 * <p>Matched by annotation name, so a consumer without {@code spring-context} still runs the rule.
 * One violation per class.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoComponentOnAbstractClassRule implements DocumentedRule {

    private static final String LOOKUP = "org.springframework.beans.factory.annotation.Lookup";

    /** Declared methods only, as Spring checks the scanned class's own metadata. */
    private static final DescribedPredicate<JavaClass> DECLARE_A_LOOKUP_METHOD = describe(
            "declare a method annotated @Lookup",
            c -> c.getMethods().stream().anyMatch(m -> m.isAnnotatedWith(LOOKUP)));

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.no-component-on-abstract-class")
            .why("""
                    Component scanning accepts a class as a bean candidate only when it is concrete — \
                    the sole exception being an abstract class that declares @Lookup methods. An \
                    abstract @Component, @Service, @Repository or @Controller therefore produces no \
                    bean definition and no warning: the scan finds the class and moves on, and the \
                    annotation stays in the source as a claim the container never honoured. Worse, it \
                    reads as if subclasses inherit it, but @Component is not @Inherited: every concrete \
                    subclass needs its own stereotype, and the one written without it is silently not \
                    a bean. The application starts, an injection point resolves to a different \
                    implementation or the feature never runs, and the missing annotation is invisible \
                    precisely because the base class appears to have one.""")
            .howToFix("""
                    Remove the stereotype from the abstract class and annotate each concrete subclass \
                    that should be a bean. If every subclass is a bean, that repetition is the honest \
                    form of the fact — or declare them as @Bean methods in a configuration class, which \
                    makes the set explicit and reviewable. If the base class was annotated so its \
                    shared dependencies would be injected, make them constructor parameters passed up \
                    by each subclass's constructor.""")
            .howNotToFix("""
                    Do NOT make the class concrete just to satisfy the scanner — that creates a bean \
                    nobody wants out of a half-implementation. Do NOT move the class to a package \
                    excluded from @ComponentScan; this check still flags it, and hiding code from the \
                    scan is not a fix. Do NOT try to move the class out of scope by naming it like a \
                    test: scope is decided by output location and declared test methods. Not caught, \
                    and no less wrong: adding an empty @Lookup method to hit Spring's carve-out, which \
                    this check cannot tell from a real one; and swapping the stereotype for \
                    @Configuration, which this check skips and which component scanning skips on an \
                    abstract class just the same.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.PRODUCTION_CLASSES)
            .and().areMetaAnnotatedWith("org.springframework.stereotype.Component")
            .and().areNotMetaAnnotatedWith("org.springframework.context.annotation.Configuration")
            .and().areNotInterfaces()
            .and(not(DECLARE_A_LOOKUP_METHOD))
            .should().haveModifier(JavaModifier.ABSTRACT);

    @ArchTest
    public static final ArchRule rule = new NoComponentOnAbstractClassRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
