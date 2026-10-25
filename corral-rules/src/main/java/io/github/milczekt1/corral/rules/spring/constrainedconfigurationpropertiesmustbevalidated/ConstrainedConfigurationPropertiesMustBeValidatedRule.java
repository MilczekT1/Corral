package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A production class bound by {@code @ConfigurationProperties} — declared on it, a superclass or an
 * interface, or on a {@code @Bean} method returning it — that declares Bean Validation constraints
 * must be {@code @Validated}, on its type hierarchy or on that method. Constraints count on the class,
 * its supertypes, their instance fields and getters, and on any non-JDK type those fields hold.
 *
 * <p>Matched by annotation name, so a consumer without Spring Boot or a Validation API still runs the
 * rule. A project constraint, a composed {@code @Validated} or a supertype outside the imported classes
 * is seen only while
 * {@code resolveMissingDependenciesFromClassPath} is left {@code true}. One violation per concrete
 * bound class and per unvalidated binding method; an abstract class or interface is reported through
 * its concrete subclasses.
 *
 * <p>Not checked: constraints on parameters or type arguments, a constraint declared only
 * {@code TYPE_USE}, a binding method declared to return a supertype of the constrained class, whether
 * the constraint package matches the Boot major, and whether {@code @Valid} reaches a nested object.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConstrainedConfigurationPropertiesMustBeValidatedRule implements DocumentedRule {

    private static final String CONFIGURATION_PROPERTIES =
            "org.springframework.boot.context.properties.ConfigurationProperties";

    private static final String VALIDATED = "org.springframework.validation.annotation.Validated";

    /** Bean Validation requires {@code @Constraint} on the constraint annotation itself, not transitively. */
    private static final List<String> CONSTRAINT_META = List.of(
            "jakarta.validation.Constraint",
            "javax.validation.Constraint");

    private static final List<String> CASCADE = List.of(
            "jakarta.validation.Valid",
            "javax.validation.Valid");

    /** Catches the built-ins and their {@code .List} containers without resolving the annotation type. */
    private static final List<String> CONSTRAINT_PACKAGES = List.of(
            "jakarta.validation.constraints.",
            "javax.validation.constraints.",
            "org.hibernate.validator.constraints.");

    private static final Pattern GETTER_NAME = Pattern.compile("(?:get|is|has)\\p{Lu}.*");

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.constrained-configuration-properties-must-be-validated")
            .why("""
                    Spring Boot runs Bean Validation on a @ConfigurationProperties binding only when it \
                    finds @Validated on the bound class, a superclass or interface of it, or the @Bean \
                    method that binds it. Without it, @NotBlank, @Min, @Pattern and @Valid on the \
                    properties class are read by nobody, and nothing is logged. An empty URL, a negative \
                    pool size or a missing API key binds without complaint, and the failure turns up \
                    later, at the first request that uses the value, instead of as a refused startup. \
                    The constraints look like protection in code review, which is why the mistake \
                    survives.""")
            .howToFix("""
                    Annotate the class with @Validated \
                    (org.springframework.validation.annotation.Validated): that covers every way the \
                    class is bound. The constraint may not be on the reported class itself: this rule also \
                    counts constraints on its superclasses and interfaces, their instance fields and \
                    getters, and any non-JDK type a field holds, at any depth, collection, map, Optional \
                    and array elements included. @Validated on a @Bean @ConfigurationProperties method covers only \
                    that binding, and satisfies this rule only when the class itself does not carry or \
                    inherit @ConfigurationProperties. Make sure a Bean Validation provider is on the \
                    runtime classpath: in a Boot application, spring-boot-starter-validation. Spring \
                    Boot 3 and later validate only jakarta.validation constraints, Boot 2 only \
                    javax.validation: a constraint from the other package is ignored even with \
                    @Validated, so change its import to match the Boot version. This rule accepts either \
                    package, so it stays green on a mismatch. Put @Valid on a field \
                    holding a nested object whose own constraints must run. From Boot 3.4 that is the \
                    only way Boot cascades into it; earlier versions, without @Valid, also skip a nested \
                    object when none of its properties is set. This rule does not check for @Valid, so \
                    a green build does not mean nested constraints run. Test classes are not checked.""")
            .howNotToFix("""
                    Do NOT delete the constraints to make the violation go away: they are the \
                    configuration's contract. Delete one only if the value really may be anything; this \
                    rule cannot tell those apart, and passes either way. Do NOT move a constraint into a \
                    type argument (List<@NotBlank String>) to make the violation go away: this rule \
                    does not see that position, so it stops reporting the class, but Boot still runs \
                    nothing there until the class is @Validated. Do NOT move a constraint from a field \
                    onto a constructor or setter parameter: Boot does not check it there even with @Validated, and this rule \
                    does not flag it. Keep it on the field; a record does this for you, and in Kotlin \
                    write @field:NotBlank. Do NOT drop @ConfigurationProperties and bind the class by \
                    hand with Binder.bind(...): nothing validates it then, and this rule cannot see it. \
                    Do NOT declare the @Bean method to return a supertype of the constrained class: \
                    this rule reads only the declared return type, so it is not caught, and the binding \
                    is still unvalidated. Do NOT add @Validated and ship without checking the existing \
                    configuration in every environment (profiles, environment variables, config \
                    server): the application that started yesterday may refuse to start today. Do NOT \
                    exclude this rule on the assumption that something else validates these classes: an \
                    exclusion switches it off for the whole build. It is right only when the \
                    application registers its own configurationPropertiesValidator bean that runs Bean \
                    Validation, which Boot applies to every binding; name that bean in the reason.""")
            .build();

    static final ArchRule DEFINITION = classes()
            .that(TestScope.PRODUCTION_CLASSES)
            .should(beValidatedWhereBoundWithConstraints());

    @ArchTest
    public static final ArchRule rule = new ConstrainedConfigurationPropertiesMustBeValidatedRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }

    private static ArchCondition<JavaClass> beValidatedWhereBoundWithConstraints() {
        return new ArchCondition<>(
                "be annotated @Validated when bound by @ConfigurationProperties with Bean Validation constraints") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                if (isValidated(type) || !declaresConstraints(type, new HashSet<>())) {
                    return;
                }
                if (isConcrete(type) && isBound(type)) {
                    events.add(SimpleConditionEvent.violated(type, "Class <" + type.getName()
                            + "> is bound by @ConfigurationProperties and declares Bean Validation constraints"
                            + " but is not annotated @Validated in " + type.getSourceCodeLocation()));
                }
                // javac copies a method's annotations onto its bridge methods.
                type.getMethodsWithReturnTypeOfSelf().stream()
                        .filter(method -> !method.getModifiers().contains(JavaModifier.SYNTHETIC))
                        .filter(method -> TestScope.PRODUCTION_CLASSES.test(method.getOwner()))
                        .filter(method -> method.isMetaAnnotatedWith(CONFIGURATION_PROPERTIES))
                        .filter(method -> !method.isMetaAnnotatedWith(VALIDATED))
                        .forEach(method -> events.add(SimpleConditionEvent.violated(method, "Method <"
                                + method.getFullName() + "> binds <" + type.getName()
                                + "> with @ConfigurationProperties but neither is annotated @Validated in "
                                + method.getSourceCodeLocation())));
            }
        };
    }

    private static Stream<JavaClass> typeHierarchy(JavaClass type) {
        return Stream.concat(type.getClassHierarchy().stream(), type.getAllRawInterfaces().stream());
    }

    private static boolean isBound(JavaClass type) {
        return typeHierarchy(type).anyMatch(supertype -> supertype.isMetaAnnotatedWith(CONFIGURATION_PROPERTIES));
    }

    private static boolean isValidated(JavaClass type) {
        return typeHierarchy(type).anyMatch(supertype -> supertype.isMetaAnnotatedWith(VALIDATED));
    }

    /** An interface is {@code ABSTRACT} in bytecode too. */
    private static boolean isConcrete(JavaClass type) {
        return !type.getModifiers().contains(JavaModifier.ABSTRACT);
    }

    /** {@code visited} terminates a type that holds itself; a revisit adds nothing the first visit missed. */
    private static boolean declaresConstraints(JavaClass type, Set<JavaClass> visited) {
        if (!visited.add(type)) {
            return false;
        }
        return typeHierarchy(type)
                .anyMatch(supertype -> hasConstraint(supertype)
                        || supertype.getFields().stream()
                                .filter(ConstrainedConfigurationPropertiesMustBeValidatedRule::isInstanceMember)
                                .anyMatch(field -> hasConstraint(field)
                                        || field.getType().getAllInvolvedRawTypes().stream()
                                                // Only spares the walk: the JDK carries no constraints.
                                                .filter(held -> !held.getName().startsWith("java."))
                                                .anyMatch(held -> declaresConstraints(held, visited)))
                        || supertype.getMethods().stream()
                                .filter(ConstrainedConfigurationPropertiesMustBeValidatedRule::isGetter)
                                .anyMatch(ConstrainedConfigurationPropertiesMustBeValidatedRule::hasConstraint));
    }

    /** Bean Validation ignores static members; a synthetic field such as {@code this$0} is not a property. */
    private static boolean isInstanceMember(JavaMember member) {
        return !member.getModifiers().contains(JavaModifier.STATIC)
                && !member.getModifiers().contains(JavaModifier.SYNTHETIC);
    }

    private static boolean isGetter(JavaMethod method) {
        return isInstanceMember(method)
                && method.getRawParameterTypes().isEmpty()
                && GETTER_NAME.matcher(method.getName()).matches();
    }

    private static boolean hasConstraint(HasAnnotations<?> element) {
        return element.getAnnotations().stream()
                .anyMatch(annotation -> isConstraintType(annotation.getRawType()) || isRepeatedConstraint(annotation));
    }

    /** A {@code @Repeatable} container of a custom constraint is not itself a {@code @Constraint}. */
    private static boolean isRepeatedConstraint(JavaAnnotation<?> annotation) {
        return annotation.get("value")
                .filter(Object[].class::isInstance)
                .stream()
                .flatMap(value -> Arrays.stream((Object[]) value))
                .filter(JavaAnnotation.class::isInstance)
                .anyMatch(element -> isConstraintType(((JavaAnnotation<?>) element).getRawType()));
    }

    private static boolean isConstraintType(JavaClass annotationType) {
        String name = annotationType.getName();
        return CONSTRAINT_PACKAGES.stream().anyMatch(name::startsWith)
                || CASCADE.contains(name)
                || CONSTRAINT_META.stream().anyMatch(annotationType::isAnnotatedWith);
    }
}
