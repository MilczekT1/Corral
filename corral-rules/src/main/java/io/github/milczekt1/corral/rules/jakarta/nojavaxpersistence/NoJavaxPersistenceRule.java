package io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may depend on anything in {@code javax.persistence..}.
 *
 * <p>Matched by package string, so a consumer without the legacy API on the classpath still
 * compiles and runs the rule.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoJavaxPersistenceRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.jakarta.no-javax-persistence")
            .why("""
                    javax.persistence.Entity and jakarta.persistence.Entity are unrelated types, and a JPA \
                    provider only sees the namespace it was compiled against. Hibernate 6 and Spring Boot 3 \
                    scan for jakarta.persistence exclusively, so a class left on javax.persistence compiles, \
                    passes its unit tests, and is simply not mapped. The failure lands far from the cause: \
                    "Not an entity" on the first query, a table the schema generator never created, a \
                    @Column(nullable = false) that was never applied. A half-migrated persistence unit is \
                    the usual shape — the entity is on jakarta, its AttributeConverter still implements \
                    the javax interface, the converter is never registered, and the column round-trips as \
                    the wrong type.""")
            .howToFix("""
                    Rewrite the import to jakarta.persistence and move the whole class at once, including \
                    every AttributeConverter, EntityManager, Query and PersistenceException reference. Then \
                    check that the build resolves jakarta.persistence:jakarta.persistence-api 3.x with \
                    Hibernate 6 or later, and that javax.persistence:javax.persistence-api is gone — find \
                    what brings it in with mvn dependency:tree. With both on the classpath either name \
                    compiles and the mixed state is invisible at build time. OpenRewrite's \
                    org.openrewrite.java.migrate.jakarta.JavaxPersistenceToJakartaPersistence recipe does \
                    the mechanical rewrite.""")
            .howNotToFix("""
                    Do NOT keep both API artifacts on the classpath so the old imports still compile: two \
                    annotation namespaces in one persistence unit is the bug, not the workaround. Do NOT \
                    swap the flagged type for a different javax.persistence one: the whole package is \
                    matched. Three things this predicate does NOT catch: a catch of a \
                    javax.persistence exception that calls nothing on it — a catch clause alone records no \
                    dependency, and the handler never runs because the new provider never throws that \
                    type; mappings and javax.persistence.* property keys in orm.xml or persistence.xml, \
                    which are not bytecode; and javax.transaction.Transactional, which is JTA rather than \
                    JPA, is not matched here, and is ignored by a jakarta transaction manager just the \
                    same.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .should().dependOnClassesThat().resideInAPackage("javax.persistence..");

    @ArchTest
    public static final ArchRule rule = new NoJavaxPersistenceRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
