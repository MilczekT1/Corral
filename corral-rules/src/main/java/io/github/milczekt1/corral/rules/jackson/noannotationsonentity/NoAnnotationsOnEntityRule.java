package io.github.milczekt1.corral.rules.jackson.noannotationsonentity;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.ANNOTATIONS;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No production class annotated {@code @Entity} or {@code @MappedSuperclass}, Jakarta or
 * {@code javax} Persistence, may depend on a Jackson annotation — on the class, a field, a method
 * or a constructor parameter. {@code @Embeddable} is not checked.
 *
 * <p>Stands alone, in no group: wire it with {@code ArchTests.in(NoAnnotationsOnEntityRule.class)}.
 * Matched by name, so it loads without Jackson or a Persistence API. It needs ArchUnit's default
 * {@code resolveMissingDependenciesFromClassPath=true}: with it off, Jackson's annotation types are
 * not recognised as annotations, and only a project's own composed annotation is reported. One
 * violation per annotation use; a Jackson annotation nested in another's member, such as
 * {@code @JsonSubTypes.Type}, is one more.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoAnnotationsOnEntityRule implements DocumentedRule {

    private static final DescribedPredicate<CanBeAnnotated> JPA_ENTITY_OR_MAPPED_SUPERCLASS =
            annotatedWith("jakarta.persistence.Entity")
                    .or(annotatedWith("jakarta.persistence.MappedSuperclass"))
                    .or(annotatedWith("javax.persistence.Entity"))
                    .or(annotatedWith("javax.persistence.MappedSuperclass"))
                    .as("are JPA entities or mapped superclasses");

    /**
     * {@code ANNOTATIONS} keeps out the enum and class types of annotation members, which are
     * dependencies too: without it one {@code @JsonFormat} reports several lines, and a Jackson
     * release adding an enum-typed member surfaces new violations in frozen code.
     */
    private static final DescribedPredicate<JavaClass> JACKSON_ANNOTATIONS = ANNOTATIONS.and(
                    resideInAnyPackage("com.fasterxml.jackson..", "tools.jackson..")
                            .or(annotatedWith("com.fasterxml.jackson.annotation.JacksonAnnotationsInside")))
            .as("are Jackson annotations");

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.jackson.no-annotations-on-entity")
            .why("""
                    Jackson annotations on an entity make one class both the database schema and the \
                    wire contract, and the two change for unrelated reasons. Renaming a column is a \
                    migration; renaming a JSON property is an API break. When the field is both, a \
                    change reviewed as a schema change ships a renamed property to every client, and \
                    adding or dropping a column adds or drops a response field. Serialising a managed \
                    entity walks its associations through their getters: with Spring Boot's default \
                    open-in-view every lazy association is loaded one SELECT at a time, without it \
                    the first uninitialised proxy fails the response, and a bidirectional association \
                    recurses forever. Binding a request body onto an entity sets whatever properties \
                    the payload names — id, version, role, balance — and the following merge() writes \
                    them: mass assignment, invisible in a controller that only says @RequestBody \
                    Order order.""")
            .howToFix("""
                    Split the type. Keep the entity as the mapping and add a response record that is \
                    the wire contract, mapped inside the transaction while associations can still \
                    load; fetch exactly what it needs with an @EntityGraph or a constructor \
                    projection. Inbound, bind to a request record holding only the fields a client \
                    may set and copy them onto the entity by hand. Move every Jackson annotation onto \
                    those records. An internal tool with a single consumer, or JPA classes a \
                    generator annotates, may keep one type: freeze those entities as debt, and the \
                    rule still stops the next entity acquiring a wire contract. Only classes \
                    annotated @Entity or @MappedSuperclass are checked; an @Embeddable is not. Test \
                    classes — by output location or a declared test method, never by name — are out \
                    of scope; a shared test-support module's main sources and Gradle testFixtures are \
                    production output and are checked.""")
            .howNotToFix("""
                    Do NOT just delete the Jackson annotations while the entity is still serialised: \
                    the rule stops firing, the failure an annotation was suppressing comes back, and \
                    this rule cannot see an entity serialised without annotations. Do NOT add \
                    @JsonIgnore to the association that failed: the next lazy association fails the \
                    same way. Do NOT add @JsonIgnoreProperties({"hibernateLazyInitializer", \
                    "handler"}): it exists only because a Hibernate proxy is being serialised. Do NOT \
                    register jackson-datatype-hibernate so uninitialised proxies serialise as null: \
                    the response changes shape with whatever the transaction happened to load, and \
                    this rule does not see it. Do NOT rely on spring.jpa.open-in-view, which Spring \
                    Boot enables by default: every lazy association becomes another query while the \
                    response is written, and this rule does not see it. Do NOT move the annotations \
                    into a mixin registered on the mapper: the wire format is still the schema, and \
                    this rule does not see mixins. Do NOT move them onto an interface or a non-entity \
                    superclass of the entity: Jackson still applies them, and this rule does not see \
                    them. Do NOT wrap them in your own annotation meta-annotated \
                    @JacksonAnnotationsInside: it is matched too, as long as ArchUnit can resolve \
                    that annotation type. Do NOT edit the freeze store.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .that(JPA_ENTITY_OR_MAPPED_SUPERCLASS).and(TestScope.PRODUCTION_CLASSES)
            .should().dependOnClassesThat(JACKSON_ANNOTATIONS);

    @ArchTest
    public static final ArchRule rule = new NoAnnotationsOnEntityRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
