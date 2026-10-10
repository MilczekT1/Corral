package io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures.ClassLevelMockBeanCase;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures.MigratedBeanOverrideCase;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures.MockBeanFieldHolder;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures.RepeatedSpyBeanCase;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures.ResetPolicyHelper;
import io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures.SpyBeanFieldHolder;
import io.github.milczekt1.corral.scope.TestScope;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Spring Boot 4 has no {@code org.springframework.boot.test.mock.mockito}, so the examples use
 * stand-ins declared under that package in this module's test sources.
 */
class NoLegacyMockBeanAndSpyBeanRuleTest {

    private static final String ID = "corral.test.spring.no-legacy-mockbean-and-spybean";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** Matched by none of {@code TestScope}'s test-output patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

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

    /** The same class file, moved: only its location changed, so only scope can explain the silence. */
    @Test
    void ignoresAFlaggedClassOnceItSitsInProductionOutput() throws IOException {
        JavaClasses moved = new ClassFileImporter().importPath(
                copyIntoProductionLayout(MockBeanFieldHolder.class));
        JavaClass holder = moved.get(MockBeanFieldHolder.class);

        assertFalse(TestScope.TEST_CLASSES.test(holder),
                "premise: outside test output and declaring no test, the class is production-scoped");
        assertTrue(holder.getDirectDependenciesFromSelf().stream()
                        .anyMatch(d -> d.getTargetClass().getPackageName()
                                .equals("org.springframework.boot.test.mock.mockito")),
                "premise: the moved class must still depend on the legacy package, or this pins nothing");

        String report = reportFor(moved);
        assertFalse(report.contains("MockBeanFieldHolder"), report);
    }

    /**
     * {@link FreezingArchRule#persistIn}, not {@code freeze.store}: a frozen rule captures its store
     * when constructed, which is class initialisation, so naming one here races class loading. Only
     * the path goes on the process-wide {@link ArchConfiguration}.
     *
     * <p>Reseed with {@code -Darchunit.freeze.store.default.allowStoreCreation=true}, then commit.
     */
    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        ArchConfiguration.get().setProperty("freeze.store.default.path", STORE_PATH);
        try {
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoLegacyMockBeanAndSpyBeanRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("MockBeanFieldHolder.clock>"), debt);
            assertTrue(debt.contains("ResetPolicyHelper"), debt);
            assertFalse(debt.contains("MigratedBeanOverrideCase"),
                    "a migrated class must never reach the store: " + debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    private Path copyIntoProductionLayout(Class<?> type) throws IOException {
        Path output = outputRoot.resolve(PRODUCTION_LAYOUT);
        Path destination = output.resolve(type.getName().replace('.', '/') + ".class");
        Files.createDirectories(destination.getParent());
        try (InputStream compiled = type.getResourceAsStream(type.getSimpleName() + ".class")) {
            assertNotNull(compiled, () -> "no compiled class file on the classpath for " + type.getName());
            Files.copy(compiled, destination);
        }
        return output;
    }
}
