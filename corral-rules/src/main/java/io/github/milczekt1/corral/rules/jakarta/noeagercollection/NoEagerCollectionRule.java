package io.github.milczekt1.corral.rules.jakarta.noeagercollection;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMembers;

import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A field or getter annotated {@code @OneToMany}, {@code @ManyToMany} or {@code @ElementCollection},
 * Jakarta or {@code javax} Persistence, must not declare {@code fetch = EAGER} and must not carry
 * Hibernate's {@code @Fetch(FetchMode.JOIN)}. Stands alone, in no group: wire it with
 * {@code ArchTests.in(NoEagerCollectionRule.class)}.
 *
 * <p>Matched by annotation name, so a consumer without a Persistence API or Hibernate still runs the
 * rule. Production and test classes alike; one violation per member.
 *
 * <p>Not checked: mappings in {@code orm.xml}, Hibernate's {@code @LazyCollection} and
 * {@code @ManyToAny}, EclipseLink's {@code @JoinFetch}, Jakarta Persistence 4.0's
 * {@code @jakarta.persistence.Fetch}, whose {@code type} overrides {@code fetch}, and fetch-profile
 * overrides.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoEagerCollectionRule implements DocumentedRule {

    private static final String JAKARTA_ONE_TO_MANY = "jakarta.persistence.OneToMany";
    private static final String JAKARTA_MANY_TO_MANY = "jakarta.persistence.ManyToMany";
    private static final String JAKARTA_ELEMENT_COLLECTION = "jakarta.persistence.ElementCollection";
    private static final String JAVAX_ONE_TO_MANY = "javax.persistence.OneToMany";
    private static final String JAVAX_MANY_TO_MANY = "javax.persistence.ManyToMany";
    private static final String JAVAX_ELEMENT_COLLECTION = "javax.persistence.ElementCollection";

    private static final List<String> COLLECTION_ANNOTATIONS = List.of(
            JAKARTA_ONE_TO_MANY, JAKARTA_MANY_TO_MANY, JAKARTA_ELEMENT_COLLECTION,
            JAVAX_ONE_TO_MANY, JAVAX_MANY_TO_MANY, JAVAX_ELEMENT_COLLECTION);

    private static final String HIBERNATE_FETCH = "org.hibernate.annotations.Fetch";

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.jakarta.no-eager-collection")
            .why("""
                    An eager collection is loaded every time its owner is. Collections default to \
                    LAZY, so fetch = FetchType.EAGER was typed on purpose, usually to silence a \
                    LazyInitializationException. Loaded by any JPQL, Criteria or Spring Data query \
                    (findAll(), derived queries, a page), Hibernate runs one extra SELECT per owner \
                    to fill the collection (fewer with @BatchSize or SUBSELECT, but still on every \
                    read): the N+1 problem. A query can add fetching but not remove it; only a fetch \
                    graph or an enabled Hibernate fetch profile (Hibernate 6.3 and later) turns the \
                    collection off, so every other query pays. Loaded by id, Hibernate outer-joins \
                    the collection and multiplies rows: two eager Sets on one entity return the \
                    product of both. Hibernate 5 refuses two eager bags (a List without \
                    @OrderColumn, or a Collection) with MultipleBagFetchException; Hibernate 6 and \
                    later load the second with a query of its own. Hibernate's \
                    @Fetch(FetchMode.JOIN) makes a collection eager whatever fetch says.""")
            .howToFix("""
                    Delete fetch = FetchType.EAGER; LAZY is the default. Remove @Fetch(FetchMode.JOIN) \
                    from the collection too: Hibernate treats it as EAGER whatever fetch says. Load \
                    the collection where it is needed: JOIN FETCH or @EntityGraph on a query that \
                    returns a bounded list, or @BatchSize on the collection. Never fetch a collection \
                    in a paginated query: Hibernate reads the whole result into memory and pages it \
                    there (HHH000104; HHH90003004 on Hibernate 6 and later). Page the owners, then \
                    fetch their collections in a second query by id. For two collections at once, \
                    fetch each in its own query in the same transaction. A \
                    LazyInitializationException after the change means code reads the collection \
                    outside a transaction, usually a serialiser or a view: fetch it in the query, or \
                    map to a DTO inside the transaction. javax.persistence takes the same fix.""")
            .howNotToFix("""
                    Do NOT put EAGER back to silence a LazyInitializationException: every query that \
                    loads the owner then pays for one call site. Do NOT rely on \
                    spring.jpa.open-in-view or hibernate.enable_lazy_load_no_trans: both run lazy \
                    loads outside the transaction, one query per access, and the rule cannot see \
                    either setting. Do NOT swap in Hibernate's \
                    @LazyCollection(LazyCollectionOption.FALSE) (Hibernate 6 and earlier) or \
                    EclipseLink's @JoinFetch, or move the mapping to orm.xml: the rule sees none of \
                    them, and the collection stays eager. Do NOT \
                    add Jakarta Persistence 4.0's @jakarta.persistence.Fetch to a collection, for a \
                    batch size or a cache mode, without type = FetchType.LAZY: its type defaults to \
                    EAGER and overrides fetch, and the rule does not see it.""")
            .build();

    static final ArchRule DEFINITION = noMembers()
            .that().areAnnotatedWith(JAKARTA_ONE_TO_MANY)
            .or().areAnnotatedWith(JAKARTA_MANY_TO_MANY)
            .or().areAnnotatedWith(JAKARTA_ELEMENT_COLLECTION)
            .or().areAnnotatedWith(JAVAX_ONE_TO_MANY)
            .or().areAnnotatedWith(JAVAX_MANY_TO_MANY)
            .or().areAnnotatedWith(JAVAX_ELEMENT_COLLECTION)
            .should(beFetchedEagerly());

    @ArchTest
    public static final ArchRule rule = new NoEagerCollectionRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }

    /** Do not name the cause in the line: it is a freeze-store key, and a member can have both. */
    private static ArchCondition<JavaMember> beFetchedEagerly() {
        return new ArchCondition<>("be fetched eagerly") {
            @Override
            public void check(JavaMember member, ConditionEvents events) {
                if (declaresEager(member) || isJoinFetched(member)) {
                    events.add(SimpleConditionEvent.satisfied(member, member.getDescription()
                            + " is a collection mapping fetched eagerly in " + member.getSourceCodeLocation()));
                }
            }
        };
    }

    /** An absent {@code fetch} is the specification default {@code LAZY}, resolved or not. */
    private static boolean declaresEager(JavaMember member) {
        return COLLECTION_ANNOTATIONS.stream()
                .flatMap(annotation -> member.tryGetAnnotationOfType(annotation).stream())
                .findFirst()
                .flatMap(collection -> enumConstantName(collection.get("fetch")))
                .filter("EAGER"::equals)
                .isPresent();
    }

    private static boolean isJoinFetched(JavaMember member) {
        return member.tryGetAnnotationOfType(HIBERNATE_FETCH)
                .flatMap(fetch -> enumConstantName(fetch.get("value")))
                .filter("JOIN"::equals)
                .isPresent();
    }

    private static Optional<String> enumConstantName(Optional<Object> value) {
        return value.filter(JavaEnumConstant.class::isInstance)
                .map(constant -> ((JavaEnumConstant) constant).name());
    }
}
