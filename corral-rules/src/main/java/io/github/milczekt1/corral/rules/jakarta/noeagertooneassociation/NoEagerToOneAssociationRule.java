package io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation;

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
 * A field or getter annotated {@code @ManyToOne} or {@code @OneToOne}, Jakarta or {@code javax}
 * Persistence, must declare {@code fetch = LAZY} and must not carry Hibernate's
 * {@code @Fetch(FetchMode.JOIN)}. Stands alone, in no group: wire it with
 * {@code ArchTests.in(NoEagerToOneAssociationRule.class)}.
 *
 * <p>Matched by annotation name, so a consumer without a Persistence API or Hibernate still runs the
 * rule. Production and test classes alike; one violation per member.
 *
 * <p>Not checked: mappings in {@code orm.xml}, Hibernate's {@code @Any} and {@code @LazyToOne},
 * {@code @NotFound}, Jakarta Persistence 4.0's {@code @jakarta.persistence.Fetch}, whose {@code type}
 * overrides {@code fetch}, and a {@code LAZY} to-one whose target entity cannot be proxied.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoEagerToOneAssociationRule implements DocumentedRule {

    private static final String JAKARTA_MANY_TO_ONE = "jakarta.persistence.ManyToOne";
    private static final String JAKARTA_ONE_TO_ONE = "jakarta.persistence.OneToOne";
    private static final String JAVAX_MANY_TO_ONE = "javax.persistence.ManyToOne";
    private static final String JAVAX_ONE_TO_ONE = "javax.persistence.OneToOne";

    private static final List<String> TO_ONE_ANNOTATIONS =
            List.of(JAKARTA_MANY_TO_ONE, JAKARTA_ONE_TO_ONE, JAVAX_MANY_TO_ONE, JAVAX_ONE_TO_ONE);

    private static final String HIBERNATE_FETCH = "org.hibernate.annotations.Fetch";

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.jakarta.no-eager-to-one-association")
            .why("""
                    @ManyToOne and @OneToOne default to FetchType.EAGER in the Persistence \
                    specification; to-many associations default to LAZY. An eager to-one is loaded \
                    every time its owner is. Loaded by id, Hibernate adds a join. Loaded by any JPQL, \
                    Criteria or Spring Data query (findAll(), derived queries), it runs one extra \
                    SELECT per distinct associated row: the N+1 problem, seen in the log as hundreds \
                    of identical single-row selects. Eager to-ones chain: OrderLine -> Order -> \
                    Customer -> Address reads four tables to load one line. JOIN FETCH and a load \
                    graph can only add fetching. Only a fetch graph (jakarta.persistence.fetchgraph, \
                    Spring Data's default @EntityGraph type) turns an eager to-one off, and only for \
                    that query, so every query without one pays. Hibernate's @Fetch(FetchMode.JOIN) \
                    makes an association eager whatever fetch says. From Jakarta Persistence 4.0 an \
                    unset fetch is FetchType.DEFAULT, which follows the persistence unit's \
                    default-to-one-fetch-type, EAGER unless configured; the rule cannot read that \
                    setting and requires fetch = LAZY on every to-one regardless.""")
            .howToFix("""
                    Declare it: @ManyToOne(fetch = FetchType.LAZY). Then fetch what each query needs \
                    with JOIN FETCH or an @EntityGraph. Do this even when the association is almost \
                    always used: "almost always" is a per-query fact and belongs at the query. Remove \
                    @Fetch(FetchMode.JOIN) from the association: it loads eagerly whatever fetch says. \
                    A LazyInitializationException after the change means code reads the association \
                    outside a transaction, usually a serialiser or a view: fetch it in the query, or \
                    map to a DTO inside the transaction. On the inverse (mappedBy) side of an \
                    optional @OneToOne, Hibernate ignores LAZY without bytecode enhancement: the rule \
                    passes, the extra query stays. Map the association on the owning side only, \
                    share the key with @MapsId, or enable bytecode enhancement. A target entity \
                    Hibernate cannot proxy, such as a final class, is also loaded immediately: the \
                    rule passes, the extra query stays. Make the target class non-final. \
                    javax.persistence takes the same fix.""")
            .howNotToFix("""
                    Do NOT put EAGER back to silence a LazyInitializationException: every other query \
                    then pays for one broken call site. Do NOT rely on spring.jpa.open-in-view \
                    (Spring Boot defaults it to true) or hibernate.enable_lazy_load_no_trans: both \
                    run lazy loads outside the transaction, one query per access, and the rule \
                    cannot see either setting. Do NOT move the mapping into orm.xml: the rule cannot \
                    see it, and the association stays eager. Do NOT add Jakarta Persistence 4.0's \
                    @jakarta.persistence.Fetch to a to-one without type = LAZY, for a batch size or a \
                    cache mode: its type defaults to EAGER and overrides fetch = LAZY, and the rule \
                    does not read it.""")
            .build();

    static final ArchRule DEFINITION = noMembers()
            .that().areAnnotatedWith(JAKARTA_MANY_TO_ONE)
            .or().areAnnotatedWith(JAKARTA_ONE_TO_ONE)
            .or().areAnnotatedWith(JAVAX_MANY_TO_ONE)
            .or().areAnnotatedWith(JAVAX_ONE_TO_ONE)
            .should(beFetchedEagerly());

    @ArchTest
    public static final ArchRule rule = new NoEagerToOneAssociationRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }

    /**
     * Do not print the {@code fetch} value: it reads {@code EAGER}, {@code DEFAULT} or absent depending
     * on the Persistence API version and whether the annotation type resolves, and the line is a
     * freeze-store key.
     */
    private static ArchCondition<JavaMember> beFetchedEagerly() {
        return new ArchCondition<>("be fetched eagerly") {
            @Override
            public void check(JavaMember member, ConditionEvents events) {
                if (!declaresLazy(member) || isJoinFetched(member)) {
                    events.add(SimpleConditionEvent.satisfied(member, member.getDescription()
                            + " is a to-one association that may be fetched eagerly in " + member.getSourceCodeLocation()));
                }
            }
        };
    }

    /** An absent {@code fetch}, like {@code EAGER} or Jakarta Persistence 4.0's {@code DEFAULT}, is not lazy. */
    private static boolean declaresLazy(JavaMember member) {
        return TO_ONE_ANNOTATIONS.stream()
                .flatMap(annotation -> member.tryGetAnnotationOfType(annotation).stream())
                .findFirst()
                .flatMap(toOne -> enumConstantName(toOne.get("fetch")))
                .filter("LAZY"::equals)
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
