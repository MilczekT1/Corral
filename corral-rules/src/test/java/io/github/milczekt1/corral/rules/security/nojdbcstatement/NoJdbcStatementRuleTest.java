package io.github.milczekt1.corral.rules.security.nojdbcstatement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures.SchemaBuilder;
import io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures.production.BankStatements;
import io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures.production.LedgerDao;
import io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures.production.OrderDao;
import io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures.production.PaymentDao;
import io.github.milczekt1.corral.rules.security.nojdbcstatement.fixtures.production.ReportExporter;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NoJdbcStatementRuleTest {

    private static final String ID = "corral.security.no-jdbc-statement";

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = ProductionLayout.importFixtures(NoJdbcStatementRuleTest.class, outputRoot.resolve("examples"));
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoJdbcStatementRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String caller) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains(caller + "(") && line.contains("createStatement")),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsTheNoArgumentOverload() {
        assertFlagged("OrderDao.byCustomer");
    }

    @Test
    void flagsTheTwoArgumentOverload() {
        assertFlagged("ReportExporter.export");
    }

    @Test
    void flagsACallThroughAConnectionSubinterface() {
        assertFlagged("LedgerDao.entries");
    }

    @Test
    void reportsOnlyTheCreateStatementCalls() {
        assertEquals(3, violationsIn(examples).size(), String.join("\n", violationsIn(examples)));
    }

    @Test
    void ignoresPrepareStatementBesideAFlaggedCall() {
        assertNotReported("OrderDao.byId");
    }

    @Test
    void ignoresBoundPreparedStatementUse() {
        assertNotReported("PaymentDao");
    }

    @Test
    void ignoresACreateStatementMethodNotOnAConnection() {
        assertNotReported("BankStatements");
    }

    @Test
    void ignoresACreateStatementCallInTestOutput() {
        assertNotReported("SchemaBuilder");
    }

    /** The same class file as above, moved: only its location changed, so only scope can explain the difference. */
    @Test
    void flagsTheTestOutputSchemaBuilderOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = ProductionLayout.copy(outputRoot.resolve("builder"), List.of(SchemaBuilder.class));
        JavaClasses builder = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(builder);

        assertTrue(violations.stream().anyMatch(line -> line.contains("SchemaBuilder.create(")),
                String.join("\n", violations));
    }

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(NoJdbcStatementRuleTest.class, NoJdbcStatementRule.DEFINITION, examples);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoJdbcStatementRule.rule, ID, examples);

        assertTrue(debt.contains("OrderDao.byCustomer("), debt);
        assertTrue(debt.contains("ReportExporter.export("), debt);
        assertTrue(debt.contains("LedgerDao.entries("), debt);
    }
}
