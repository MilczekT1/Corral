package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseRulesTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.archrules.fixtures.database");

    private static List<String> violations(ArchRule rule) {
        return rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails();
    }

    private static String joined(ArchRule rule) {
        return String.join("\n", violations(rule));
    }

    @Test
    void flagsClassLevelTransactional() {
        String report = joined(DatabaseRules.NO_TX_ON_CLASSES_RULE);

        assertTrue(report.contains("AnnotatedService"), report);
        assertFalse(report.contains("CleanService"), report);
    }

    @Test
    void flagsMethodLevelTransactional() {
        String report = joined(DatabaseRules.NO_TX_ON_METHODS_RULE);

        assertTrue(report.contains("AnnotatedMethodService"), report);
        assertTrue(report.contains("doWork"), report);
        assertFalse(report.contains("untouched"), report);
    }

    @Test
    void classLevelRuleDoesNotDoubleReportMethodAnnotations() {
        String report = joined(DatabaseRules.NO_TX_ON_CLASSES_RULE);

        assertFalse(report.contains("AnnotatedMethodService"),
                "the class-level rule must only match class-level annotations: " + report);
    }

    @Test
    void flagsRawJdbcOutsideRepositoryPackages() {
        String report = joined(DatabaseRules.NO_RAW_JDBC_RULE);

        assertTrue(report.contains("RawJdbcService"), report);
    }

    @Test
    void allowsRawJdbcInsideRepositoryPackages() {
        String report = joined(DatabaseRules.NO_RAW_JDBC_RULE);

        assertFalse(report.contains("OrderRepository"),
                "repository packages are the sanctioned home for JDBC: " + report);
    }

    @Test
    void everyPublicRuleIsFrozenAndIdPinned() {
        assertEquals("db.no-spring-transactional-on-classes",
                DatabaseRules.noSpringTransactionalOnClasses.getDescription());
        assertEquals("db.no-spring-transactional-on-methods",
                DatabaseRules.noSpringTransactionalOnMethods.getDescription());
        assertEquals("db.no-raw-jdbc-outside-repositories",
                DatabaseRules.noRawJdbcOutsideRepositories.getDescription());
    }
}
