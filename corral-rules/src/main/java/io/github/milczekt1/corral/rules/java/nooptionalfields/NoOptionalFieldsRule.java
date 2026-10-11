package io.github.milczekt1.corral.rules.java.nooptionalfields;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No instance field of a production class may be typed {@code Optional}, {@code OptionalInt},
 * {@code OptionalLong} or {@code OptionalDouble}, unless the declaring class is annotated or
 * meta-annotated {@code @Component}.
 *
 * <p>Stands alone, in no group: wire it with {@code ArchTests.in(NoOptionalFieldsRule.class)}. Only the
 * raw field type is checked, so {@code List<Optional<T>>} is not flagged. The stereotype is matched by
 * name, so a consumer without {@code spring-context} still runs the rule, with nothing exempt. When
 * ArchUnit cannot resolve {@code @Service} or {@code @Configuration} — {@code spring-context} absent
 * from the test classpath, or {@code resolveMissingDependenciesFromClassPath=false} — their classes
 * are not recognised as stereotypes and are flagged. One violation per field.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoOptionalFieldsRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.java.no-optional-fields")
            .why("""
                    Optional is designed as a method return type: the JDK's own API note says it is \
                    "primarily intended for use as a method return type" and that a variable of type \
                    Optional "should never itself be null". A field breaks that, with concrete \
                    defects. None of Optional, OptionalInt, OptionalLong or OptionalDouble implements \
                    Serializable, so an object holding one fails with NotSerializableException the \
                    first time it reaches a session store, a cache or a remote call — at runtime, \
                    rarely in a unit test. JPA has no basic mapping for Optional, so the provider \
                    rejects the attribute. And a field defaults to null: any construction path that \
                    does not assign it — a JPA no-arg constructor, a deserializer that skips an absent \
                    property, a constructor added later — leaves it null, and the first isPresent() \
                    throws the NullPointerException the type was meant to prevent. Fields of a Spring \
                    stereotype bean are exempt: there an Optional<T> dependency is how Spring \
                    expresses an optional collaborator, the container assigns Optional.empty() when no \
                    bean matches, and a singleton bean is neither serialized nor persisted. A \
                    session-scoped stereotype bean is serialized with its session, and its Optional \
                    fields are not caught.""")
            .howToFix("""
                    Store the value as its plain, nullable type and return Optional.ofNullable(field) \
                    from the accessor, so the Optional exists only at the boundary where it tells the \
                    caller that absence is a normal outcome. For a primitive variant, store the boxed \
                    type (Integer, Long, Double) and return OptionalInt.empty() or OptionalInt.of(value) \
                    from the accessor. For a record, make the component the plain type and annotate it \
                    @Nullable; a record's accessor must return the component's own type, so if callers \
                    need an Optional, add a separately named method that wraps the component. A field \
                    of a class annotated or meta-annotated @Component — @Service, @Repository, \
                    @Controller, @RestController, @Configuration — is not flagged, however it is \
                    injected. A bean whose class carries no stereotype — one created by a @Bean method, \
                    registered through @Import, or scanned through @Named or @ManagedBean — is still \
                    flagged: for its optional dependency, keep the Optional<T> constructor parameter \
                    and store dependency.orElse(null) in a @Nullable field, or inject Spring's \
                    ObjectProvider<T> and call getIfAvailable() where the dependency is used. Static \
                    fields, compiler-generated fields, return types, parameters, local variables and \
                    test classes are outside this rule. A shared test-support module's main sources \
                    and Gradle testFixtures are production output, and are flagged.""")
            .howNotToFix("""
                    Do NOT annotate a class @Component, or any stereotype, to get it past this check: \
                    the exemption is by annotation alone, so it will not notice. Inside a scanned \
                    package Spring then creates a singleton of a class that was never a bean, and fails \
                    at startup if its constructor takes values rather than beans. Do NOT make the \
                    field static: static fields are not checked, so this is not caught, and the value \
                    is then shared by every instance. Do NOT mark the field transient: it is still \
                    flagged, and on deserialization it comes back null — the null-Optional failure \
                    above. Do NOT keep the field and register Jackson's Jdk8Module instead: the field \
                    is still flagged, and that fixes JSON only, not Java serialization or JPA. Do NOT \
                    remove the Optional from the accessor as well, leaving a raw nullable return: \
                    return types are not checked, so this is not caught, but the return type is the \
                    one place Optional belongs. Do NOT switch to your own Maybe or Option type, Guava's \
                    Optional or Vavr's Option: only the four JDK types are matched, so this is not \
                    caught, and a wrapper field still defaults to null. Do NOT move the Optional into a \
                    type argument (List<Optional<T>>, AtomicReference<Optional<T>>): only the raw field \
                    type is checked, so this is not caught. Do NOT try to move the class out of scope \
                    by naming it like a test: scope is decided by output location and declared test \
                    methods.""")
            .build();

    static final ArchRule DEFINITION = noFields()
            .that().areDeclaredInClassesThat(TestScope.PRODUCTION_CLASSES)
            .and().areNotStatic()
            .and().doNotHaveModifier(JavaModifier.SYNTHETIC)
            .and().areDeclaredInClassesThat().areNotMetaAnnotatedWith("org.springframework.stereotype.Component")
            .should().haveRawType(type(Optional.class)
                    .or(type(OptionalInt.class))
                    .or(type(OptionalLong.class))
                    .or(type(OptionalDouble.class))
                    .as("Optional, OptionalInt, OptionalLong or OptionalDouble"));

    @ArchTest
    public static final ArchRule rule = new NoOptionalFieldsRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
