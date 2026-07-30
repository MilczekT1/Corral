package io.github.milczekt1.archrules.groups;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.archrules.FrozenRules;
import io.github.milczekt1.archrules.RuleDoc;

/**
 * Rules about how code talks to the database.
 *
 * <p>Each rule appears twice: a package-private raw {@code *_RULE} constant holding the bare
 * predicate (unit-tested directly), and the public {@code @ArchTest} field consumers run, which is
 * registered, id-pinned and frozen by {@link FrozenRules#freeze}. Only the annotated fields are
 * picked up by {@code ArchTests.in(DatabaseRules.class)}.
 */
public final class DatabaseRules {

    /** Matched by FQN string so the framework needs no Spring on its classpath. */
    private static final String SPRING_TRANSACTIONAL =
            "org.springframework.transaction.annotation.Transactional";

    private static final String JDBC_TEMPLATE = "org.springframework.jdbc.core.JdbcTemplate";

    /** Packages where hand-written SQL/JDBC is the sanctioned, reviewed home. */
    private static final String[] PERSISTENCE_PACKAGES = {
            "..repository..", "..repositories..", "..dao..", "..jdbc..", "..persistence.."
    };

    private DatabaseRules() {
    }

    // ---------------------------------------------------------------- no @Transactional on classes

    static final RuleDoc NO_TX_ON_CLASSES_DOC = RuleDoc.builder()
            .id("db.no-spring-transactional-on-classes")
            .why("Declarative @Transactional gives you no control over retries, and cannot express the "
                    + "retry-on-serialization-failure semantics that distributed SQL databases require. It also "
                    + "hides transaction boundaries behind a proxy, so self-invocation silently runs "
                    + "un-transacted.")
            .howToFix("Make the transaction boundary explicit with a programmatic wrapper — your project's "
                    + "Transactor, Spring's TransactionTemplate, or the equivalent — and wrap only the work "
                    + "that must be atomic.")
            .howNotToFix("Do NOT swap in a different flavour such as @Transactional(propagation = REQUIRES_NEW), "
                    + "readOnly = true, or a custom meta-annotation that is itself annotated with @Transactional. "
                    + "Every variant is banned; the problem is declarative transaction management, not one "
                    + "attribute of it.")
            .build();

    static final ArchRule NO_TX_ON_CLASSES_RULE = noClasses()
            .should().beAnnotatedWith(SPRING_TRANSACTIONAL);

    @ArchTest
    public static final ArchRule noSpringTransactionalOnClasses =
            FrozenRules.freeze(NO_TX_ON_CLASSES_RULE, NO_TX_ON_CLASSES_DOC);

    // ---------------------------------------------------------------- no @Transactional on methods

    static final RuleDoc NO_TX_ON_METHODS_DOC = RuleDoc.builder()
            .id("db.no-spring-transactional-on-methods")
            .why("Method-level @Transactional has the same problem as the class-level form — no retry "
                    + "control, proxy-bound boundaries — and is the more common way it sneaks back in.")
            .howToFix("Replace the annotation with an explicit programmatic transaction around the work the "
                    + "method performs.")
            .howNotToFix("Do NOT move the annotation up to the class, down to a helper, or onto an interface "
                    + "method to get it out of this rule's way. It is banned in every position.")
            .build();

    static final ArchRule NO_TX_ON_METHODS_RULE = noMethods()
            .should().beAnnotatedWith(SPRING_TRANSACTIONAL);

    @ArchTest
    public static final ArchRule noSpringTransactionalOnMethods =
            FrozenRules.freeze(NO_TX_ON_METHODS_RULE, NO_TX_ON_METHODS_DOC);

    // ---------------------------------------------------------------- no raw JDBC outside repositories

    static final RuleDoc NO_RAW_JDBC_DOC = RuleDoc.builder()
            .id("db.no-raw-jdbc-outside-repositories")
            .why("Raw JDBC scattered through services leaks persistence concerns into business logic, "
                    + "bypasses the connection and retry handling the persistence layer applies, and makes "
                    + "SQL impossible to review in one place.")
            .howToFix("Move the query behind a type in a repository, dao, jdbc, or persistence package and "
                    + "call that from the service.")
            .howNotToFix("Do NOT rename the offending class or move it into a package merely named "
                    + "'repository' while it keeps doing service work — the package boundary is meant to "
                    + "reflect a real layering decision, not to satisfy a matcher.")
            .build();

    static final ArchRule NO_RAW_JDBC_RULE = noClasses()
            .that().resideOutsideOfPackages(PERSISTENCE_PACKAGES)
            .should().dependOnClassesThat().resideInAnyPackage("java.sql..", "javax.sql..")
            .orShould().dependOnClassesThat().haveFullyQualifiedName(JDBC_TEMPLATE);

    @ArchTest
    public static final ArchRule noRawJdbcOutsideRepositories =
            FrozenRules.freeze(NO_RAW_JDBC_RULE, NO_RAW_JDBC_DOC);
}
