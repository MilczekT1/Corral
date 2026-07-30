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
 * Pins every public {@code @ArchTest} field of {@link TestingRules} to the raw rule it wraps.
 * See {@link FrozenFieldStores} for why {@code getDescription()} alone is not enough.
 */
class TestingRulesFrozenFieldsTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.archrules.fixtures.testing");

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
    void integrationTestsMustNotMockRepositoriesOrDaosFreezesItsOwnRawRule() {
        FrozenFieldStores.assertFreezes(TestingRules.integrationTestsMustNotMockRepositoriesOrDaos,
                TestingRules.NO_MOCKED_REPOS_IN_IT_RULE, TestingRules.NO_MOCKED_REPOS_IN_IT_DOC,
                FIXTURES, store);
    }

    @Test
    void testClassNamingConventionFreezesItsOwnRawRule() {
        FrozenFieldStores.assertFreezes(TestingRules.testClassNamingConvention,
                TestingRules.TEST_NAMING_RULE, TestingRules.TEST_NAMING_DOC, FIXTURES, store);
    }

    @Test
    void everyPublishedFieldOfThisGroupIsCovered() {
        // If a rule is added to TestingRules without a pairing test above, this fails.
        assertEquals(2, PublishedRules.archRuleFieldsOf(TestingRules.class).size(),
                "add a pairing test for the new TestingRules field");
    }
}
