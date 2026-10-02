package io.github.milczekt1.corral.rules.security.nojdbcstatement;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No production class may call {@code Connection.createStatement}.
 *
 * <p>Sees the call, never the SQL string: a {@code PreparedStatement} built from a concatenated
 * string, and a library such as {@code JdbcTemplate} handed one, pass this rule.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoJdbcStatementRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.security.no-jdbc-statement")
            .why("""
                    A plain Statement has exactly one way to receive SQL: a String the caller has \
                    already finished building. Whatever produced it — concatenation, String.format, a \
                    template — ran before the driver saw it, so there is no boundary left between the \
                    query the developer wrote and the data a user supplied. A value containing \
                    ' OR 1=1-- is not escaped badly; it is not escaped at all, because by then it is \
                    query text. A PreparedStatement removes the vector structurally: the SQL and the \
                    parameters travel separately, the statement is parsed with its ? placeholders in \
                    place, and a bound parameter can never be re-read as syntax, whatever it contains.""")
            .howToFix("""
                    Replace connection.createStatement() and executeQuery(sql) with \
                    connection.prepareStatement(sql), where sql is a constant containing ? \
                    placeholders, then bind each value with its typed setter — setString, setLong, \
                    setTimestamp. The binding is what makes the value inert; the PreparedStatement type \
                    on its own does not. For the parts of a query a placeholder cannot express — a sort \
                    column, a table name, an IN list of varying length — validate the fragment against \
                    a fixed allowlist of identifiers you control, or generate exactly as many ? \
                    placeholders as there are values and bind each one. If the statement is DDL or a \
                    constant with no input in it — CREATE TABLE, ALTER, a one-shot SET — it is a \
                    legitimate exception: exclude it with a reason that says so.""")
            .howNotToFix("""
                    Do NOT switch to prepareStatement while still concatenating the value into the SQL \
                    string: that is exactly as injectable as a Statement, and this rule does not catch \
                    it, because the call it sees is now the right one. Do NOT move the query to an API \
                    that takes a finished string — JdbcTemplate's execute, query, update or \
                    queryForList with a concatenated string and no arguments, or a JPA createQuery or \
                    createNativeQuery built by concatenation: the Statement is then created inside the \
                    library, this rule does not catch it either, and the vector is unchanged. Do NOT \
                    keep createStatement and sanitise the input by stripping quotes, escaping \
                    apostrophes or filtering SQL keywords — every such filter has been bypassed. Do NOT \
                    move the call into a helper class: it is matched wherever it is declared in \
                    production code. Do NOT try to move the class out of scope by naming it like a \
                    test: scope is decided by output location and declared test methods. Green here \
                    means no createStatement call was found, not that the codebase is free of SQL \
                    injection.""")
            .build();

    /**
     * Owner and name, not signature: every overload, and a call through a driver's own
     * {@code Connection} subinterface. Named by string so a runtime without the {@code java.sql}
     * module still loads the rule.
     */
    private static final DescribedPredicate<JavaCall<?>> CREATE_STATEMENT_CALL =
            target(owner(assignableTo("java.sql.Connection")))
                    .and(target(name("createStatement")))
                    .as("target is Connection.createStatement");

    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.PRODUCTION_CLASSES)
            .should().callMethodWhere(CREATE_STATEMENT_CALL);

    @ArchTest
    public static final ArchRule rule = new NoJdbcStatementRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
