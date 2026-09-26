package io.github.milczekt1.corral.rules.jakarta.nojavaxvalidation;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may depend on anything in {@code javax.validation..}.
 *
 * <p>Matched by package string, so a consumer without the legacy API on the classpath still
 * compiles and runs the rule.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoJavaxValidationRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.jakarta.no-javax-validation")
            .why("""
                    Bean Validation constraints fail open. A validator looks for annotations in the \
                    namespace it knows, and an annotation from any other namespace is not invalid, it is \
                    invisible. Under Hibernate Validator 8 or Spring Boot 3, @javax.validation.constraints \
                    .NotNull on a request DTO is decoration: the endpoint accepts null, the @Valid \
                    parameter passes, and the null travels on into the service layer as an NPE, a null \
                    column, or a row a database constraint rejects hours later. Nothing warns and no test \
                    fails unless one asserts the rejection. Custom constraints fail the same way: a \
                    @Constraint meta-annotation still on javax.validation is not discovered, and a \
                    ConstraintValidator implementing the old interface is never resolved against the new \
                    one, because they are unrelated types.""")
            .howToFix("""
                    Rewrite the import to jakarta.validation and move the whole class at once, including \
                    every @Constraint, ConstraintValidator, ConstraintViolationException and \
                    ValidationException reference. Then check that the build resolves \
                    jakarta.validation:jakarta.validation-api 3.x with Hibernate Validator 8 or later, and \
                    that javax.validation:validation-api is gone — find what brings it in with mvn \
                    dependency:tree. With both on the classpath either name compiles and the mixed state \
                    is invisible at build time. Finally, write one test asserting that a constraint you \
                    migrated rejects a bad value: a constraint that silently stopped working is exactly \
                    what this rename produces, and it throws nothing.""")
            .howNotToFix("""
                    Do NOT keep both API artifacts on the classpath so the old imports still compile. Do \
                    NOT add a manual null check in the service and leave the annotation on the old \
                    namespace — the annotation is then a lie about where validation happens. Do NOT swap \
                    the flagged constraint for a different javax.validation one: the whole package is \
                    matched. Two things this predicate does NOT catch, and both are as dead at runtime \
                    as what it does: a catch of javax.validation.ConstraintViolationException that calls \
                    nothing on the exception — a catch clause alone records no dependency, and the \
                    handler never runs because the new validator never throws that type — and \
                    constraints declared in an XML mapping (META-INF/validation.xml or a constraint \
                    mapping file), which are not bytecode.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .should().dependOnClassesThat().resideInAPackage("javax.validation..");

    @ArchTest
    public static final ArchRule rule = new NoJavaxValidationRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
