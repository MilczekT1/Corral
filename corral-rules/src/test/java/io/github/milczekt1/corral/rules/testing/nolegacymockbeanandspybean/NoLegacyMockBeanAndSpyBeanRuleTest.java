package io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures.ClassLevelMockBeanCase;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures.MigratedBeanOverrideCase;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures.MockBeanFieldHolder;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures.RepeatedSpyBeanCase;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures.ResetPolicyHelper;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures.SpyBeanFieldHolder;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import io.github.milczekt1.corral.scope.TestScope;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NoLegacyMockBeanAndSpyBeanRuleTest {

    private static final String ID = "corral.test.spring.no-legacy-mockbean-and-spybean";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            MockBeanFieldHolder.class, SpyBeanFieldHolder.class, ClassLevelMockBeanCase.class,
            RepeatedSpyBeanCase.class, ResetPolicyHelper.class, MigratedBeanOverrideCase.class);

    @TempDir
    Path outputRoot;

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String reportFor(JavaClasses classes) {
        return String.join("\n", NoLegacyMockBeanAndSpyBeanRule.DEFINITION
                .allowEmptyShould(true).evaluate(classes).getFailureReport().getDetails());
    }

    static Stream<Arguments> flags() {
        return Stream.of(
                arguments("a @MockBean field", "MockBeanFieldHolder.clock>", ".mockito.MockBean>"),
                arguments("a @SpyBean field", "SpyBeanFieldHolder.clock>", ".mockito.SpyBean>"),
                arguments("a class-level @MockBean", "ClassLevelMockBeanCase>", ".mockito.MockBean>"),
                arguments("the @SpyBeans container", "RepeatedSpyBeanCase>", ".mockito.SpyBeans>"),
                arguments("a support type with no annotation", "ResetPolicyHelper.policy()", ".mockito.MockReset"));
    }

    @ParameterizedTest(name = "flags {0}")
    @MethodSource
    void flags(String description, String offender, String legacyType) {
        String report = reportFor(EXAMPLES);

        assertTrue(report.lines().anyMatch(line -> line.contains(offender) && line.contains(legacyType)),
                () -> "must flag " + description + ": " + report);
    }

    @Test
    void ignoresTheReplacementFieldsBesideAFlaggedOne() {
        String report = reportFor(EXAMPLES);

        assertFalse(report.contains("replacementClock"), report);
    }

    @Test
    void ignoresAClassUsingOnlyTheReplacementsAndPlainMockito() {
        String report = reportFor(EXAMPLES);

        assertFalse(report.contains("MigratedBeanOverrideCase"), report);
    }

    /** A shared test-support module's main sources: Boot 4 fails to compile those too. */
    @Test
    void flagsAClassInProductionOutputToo() throws IOException {
        JavaClasses moved = new ClassFileImporter().importPath(
                ProductionLayout.copy(outputRoot, List.of(MockBeanFieldHolder.class)));

        assertFalse(TestScope.TEST_CLASSES.test(moved.get(MockBeanFieldHolder.class)),
                "premise: outside test output and declaring no test, the class is production-scoped");

        String report = reportFor(moved);
        assertTrue(report.contains("MockBeanFieldHolder.clock>"), report);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoLegacyMockBeanAndSpyBeanRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("MockBeanFieldHolder.clock>"), debt);
        assertTrue(debt.contains("ResetPolicyHelper"), debt);
        assertFalse(debt.contains("MigratedBeanOverrideCase"),
                "a migrated class must never reach the store: " + debt);
    }
}
