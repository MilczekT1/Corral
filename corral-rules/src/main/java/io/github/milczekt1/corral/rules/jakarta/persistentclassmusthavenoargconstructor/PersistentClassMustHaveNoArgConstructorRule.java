package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.RECORDS;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A class annotated {@code @Entity} or {@code @Embeddable}, Jakarta or {@code javax} Persistence, must
 * declare a no-arg constructor that is not {@code private}. A Jakarta {@code @Embeddable} is exempt
 * when it is a record, carries {@code @EmbeddableInstantiator} directly, or is public with a public
 * constructor annotated {@code @Instantiator} — Hibernate 6.2+ builds those without one. Hibernate
 * calls an {@code @Instantiator} constructor without making it accessible. Do not widen the
 * {@code @EmbeddableInstantiator} check to meta-annotations: Hibernate never reads it through a
 * composed annotation.
 *
 * <p>Matched by annotation name, so a consumer without a Persistence API or Hibernate still runs the
 * rule. Production and test classes alike; one violation per class.
 *
 * <p>Not checked: entities mapped only in {@code orm.xml}, {@code @IdClass} targets, a value class
 * with no {@code @Embeddable} of its own, mapped through {@code @Embedded} or {@code @EmbeddedId} on
 * the attribute, an {@code @EmbeddedId} class relying on its {@code @EmbeddableInstantiator}, which
 * Hibernate ignores on a composite id, and a Jakarta record embeddable on a provider without record
 * support, such as Hibernate before 6.2. Flagged although Hibernate can build it: an embeddable whose
 * {@code @EmbeddableInstantiator} is only on the embedding attribute or registered with
 * {@code @EmbeddableInstantiatorRegistration}. Flagged although Hibernate never instantiates it: an
 * abstract {@code @Embeddable}, such as the root of a Hibernate 6.6+ embeddable hierarchy.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PersistentClassMustHaveNoArgConstructorRule implements DocumentedRule {

    private static final DescribedPredicate<JavaClass> ENTITIES =
            annotatedWith("jakarta.persistence.Entity").<JavaClass>forSubtype()
                    .or(annotatedWith("javax.persistence.Entity"));

    private static final DescribedPredicate<JavaClass> JAVAX_EMBEDDABLES =
            annotatedWith("javax.persistence.Embeddable").forSubtype();

    private static final DescribedPredicate<JavaClass> JAKARTA_EMBEDDABLES =
            annotatedWith("jakarta.persistence.Embeddable").forSubtype();

    private static final DescribedPredicate<JavaClass> BUILT_WITHOUT_A_NO_ARG_CONSTRUCTOR = RECORDS
            .or(annotatedWith("org.hibernate.annotations.EmbeddableInstantiator"))
            .or(declareAPublicConstructorAnnotatedWith("org.hibernate.annotations.Instantiator"));

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.jakarta.persistent-class-must-have-no-arg-constructor")
            .why("""
                    A JPA provider builds an entity or embeddable by calling its no-arg constructor and \
                    then setting the fields from the row; the JPA specification requires that \
                    constructor, public or protected. Without one the build is green and the \
                    application boots — Hibernate logs only an INFO line at startup (HHH000182) — and \
                    the first load, or merge of a detached instance, throws InstantiationException: \
                    "No default constructor for entity", or "Unable to locate constructor for \
                    embeddable". A write-only entity can ship and fail months later, when someone adds \
                    a query. A private no-arg constructor loads but breaks lazy loading: Hibernate's \
                    lazy proxy is a generated subclass, so it needs the constructor at least \
                    package-private. A record cannot be an entity at all: it is final and its fields \
                    cannot be set after construction.""")
            .howToFix("""
                    Add protected Order() {}, or @NoArgsConstructor(access = AccessLevel.PROTECTED) \
                    with Lombok, and keep the invariant-enforcing constructor alongside it. protected \
                    lets the provider and its proxies in and keeps application code from building an \
                    empty instance; package-private passes too. With Lombok @Builder, add both \
                    @NoArgsConstructor(access = AccessLevel.PROTECTED) and @AllArgsConstructor: \
                    @Builder stops generating its own constructor once another exists, and the build \
                    breaks. Replace @Value with @Getter: @Value makes the class and its fields final, \
                    and @NoArgsConstructor does not compile on final fields. In Kotlin, apply the \
                    kotlin-jpa (no-arg) compiler plugin. A jakarta.persistence.Embeddable value object \
                    on Hibernate 6.2+ may instead be a record, or annotate its invariant-enforcing \
                    constructor @org.hibernate.annotations.Instantiator({"amount", "currency"}), naming \
                    the persistent attribute each parameter sets, in parameter order; the rule accepts \
                    both. That constructor and its class must be public: Hibernate calls it \
                    reflectively without making it accessible, so a protected one throws on load. \
                    @Instantiator is @Incubating in Hibernate. An @EmbeddableInstantiator on the \
                    embedding attribute, or registered with @EmbeddableInstantiatorRegistration, is not \
                    seen: annotate the embeddable class itself instead. On javax Persistence none of \
                    these work: add the no-arg constructor. If an @Entity is a \
                    record, make it a class, or keep the record as a DTO mapped from a separate entity \
                    class.""")
            .howNotToFix("""
                    Do NOT delete the invariant-enforcing constructor to get the implicit default one \
                    back: the rule passes either way, so keep both. Do NOT make the no-arg constructor \
                    public when protected works: the rule accepts public, but it lets any code build an \
                    empty entity. Do NOT add a no-arg constructor to a record @Entity, or keep @Value \
                    by adding @NoArgsConstructor(force = true): the rule then passes, but the class \
                    stays final — a record still cannot be an entity, and Hibernate cannot proxy a \
                    final class. Do NOT replace the constructor on an @Entity with a custom \
                    instantiator or unsafe allocation: the rule still flags it. Do NOT add the \
                    violation to the freeze store by hand: an entity that cannot be loaded is a bug \
                    about to ship, not inherited debt.""")
            .build();

    static final ArchRule DEFINITION = classes()
            .that(ENTITIES.or(JAVAX_EMBEDDABLES).or(JAKARTA_EMBEDDABLES.and(not(BUILT_WITHOUT_A_NO_ARG_CONSTRUCTOR)))
                    .as("are JPA entities, or embeddables Hibernate cannot build another way"))
            .should(declareANonPrivateNoArgConstructor());

    @ArchTest
    public static final ArchRule rule = new PersistentClassMustHaveNoArgConstructorRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }

    private static DescribedPredicate<JavaClass> declareAPublicConstructorAnnotatedWith(String annotation) {
        return DescribedPredicate.describe("are public and declare a public constructor annotated with @" + annotation,
                javaClass -> javaClass.getModifiers().contains(JavaModifier.PUBLIC)
                        && javaClass.getConstructors().stream().anyMatch(constructor ->
                                constructor.getModifiers().contains(JavaModifier.PUBLIC)
                                        && constructor.isAnnotatedWith(annotation)));
    }

    /** Must not skip {@code SYNTHETIC}: the Kotlin no-arg plugin's constructor is synthetic. */
    private static ArchCondition<JavaClass> declareANonPrivateNoArgConstructor() {
        return new ArchCondition<>("declare a non-private no-arg constructor") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean satisfied = javaClass.getConstructors().stream().anyMatch(constructor ->
                        constructor.getRawParameterTypes().isEmpty()
                                && !constructor.getModifiers().contains(JavaModifier.PRIVATE));
                if (!satisfied) {
                    events.add(SimpleConditionEvent.violated(javaClass, "Class <" + javaClass.getFullName()
                            + "> does not declare a non-private no-arg constructor in "
                            + javaClass.getSourceCodeLocation()));
                }
            }
        };
    }
}
