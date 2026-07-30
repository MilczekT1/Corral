package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.archrules.testsupport.PublishedRules;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pins every public {@code @ArchTest} field of {@link DatabaseRules} to the raw rule it wraps.
 * See {@link FrozenFieldStores} for why {@code getDescription()} alone is not enough.
 */
class DatabaseRulesFrozenFieldsTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.archrules.fixtures.database");

    @TempDir
    Path store;

    @BeforeEach
    void useTemporaryStore() {
        FrozenFieldStores.useTemporaryStore(store);
    }

    /** ArchConfiguration is global and Surefire reuses one JVM; a leaked store path corrupts siblings. */
    @AfterEach
    void resetConfiguration() {
        FrozenFieldStores.resetConfiguration();
    }

    @Test
    void noSpringTransactionalOnClassesFreezesItsOwnRawRule() {
        FrozenFieldStores.assertFreezes(DatabaseRules.noSpringTransactionalOnClasses,
                DatabaseRules.NO_TX_ON_CLASSES_RULE, DatabaseRules.NO_TX_ON_CLASSES_DOC, FIXTURES, store);
    }

    @Test
    void noSpringTransactionalOnMethodsFreezesItsOwnRawRule() {
        FrozenFieldStores.assertFreezes(DatabaseRules.noSpringTransactionalOnMethods,
                DatabaseRules.NO_TX_ON_METHODS_RULE, DatabaseRules.NO_TX_ON_METHODS_DOC, FIXTURES, store);
    }

    @Test
    void noRawJdbcOutsideRepositoriesFreezesItsOwnRawRule() {
        FrozenFieldStores.assertFreezes(DatabaseRules.noRawJdbcOutsideRepositories,
                DatabaseRules.NO_RAW_JDBC_RULE, DatabaseRules.NO_RAW_JDBC_DOC, FIXTURES, store);
    }

    @Test
    void everyPublishedFieldOfThisGroupIsCovered() {
        // If a rule is added to DatabaseRules without a pairing test above, this fails.
        assertEquals(3, PublishedRules.archRuleFieldsOf(DatabaseRules.class).size(),
                "add a pairing test for the new DatabaseRules field");
    }
}
