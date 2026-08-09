package io.github.milczekt1.llamarules.rules.rules.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.llamarules.rules.groups.FrozenFieldStores;
import io.github.milczekt1.llamarules.rules.groups.TestingRules;
import io.github.milczekt1.llamarules.rules.testsupport.PublishedRules;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pins every public {@code @ArchTest} field reachable from {@link TestingRules} to the raw rule it
 * wraps. See {@link FrozenFieldStores} for why {@code getDescription()} alone is not enough.
 */
class TestingRulesFrozenFieldsTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.llamarules.rules.fixtures.testing");

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
        FrozenFieldStores.assertFreezes(NoMockedRepositoryInIntegrationTest.rule,
                NoMockedRepositoryInIntegrationTest.RULE, NoMockedRepositoryInIntegrationTest.DOC,
                FIXTURES, store);
    }

    @Test
    void testClassNamingConventionFreezesItsOwnRawRule() {
        FrozenFieldStores.assertFreezes(TestClassNamingConvention.rule,
                TestClassNamingConvention.RULE, TestClassNamingConvention.DOC, FIXTURES, store);
    }

    /** One pairing test per rule above; keep in step when adding one. */
    private static final int PAIRING_TESTS = 2;

    @Test
    void everyPublishedFieldOfThisGroupIsCovered() {
        // If a rule is added to TestingRules without a pairing test above, this fails. Derived from
        // members() rather than hardcoded twice, so the group itself is the single source of truth
        // for how many rules there are.
        assertEquals(TestingRules.members().size(), PublishedRules.rulesReachableFrom(TestingRules.class).size(),
                "every TestingRules member must publish exactly one rule");
        assertEquals(PAIRING_TESTS, TestingRules.members().size(),
                "add a pairing test for the new TestingRules rule");
    }
}
