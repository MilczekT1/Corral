package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.CacheProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.CatalogProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ChannelProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ClientConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ClientSettings;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ConsistencyProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.Datasource;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.IgnoredMembersProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.JavaxCascadeProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.JavaxProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.LegacyCodeProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.PaymentProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.PoolProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.QuorumProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.RetryPolicy;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.RetryProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ScheduleProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.SearchIndex;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.SearchProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.SettingsHolder;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.SmsChannelProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.TestOutputConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.TestOutputProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.TlsClientConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.TlsClientSettings;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.TreeProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ValidatedSettings;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The rule is scoped to production classes, and every fixture compiles into test output. So the
 * fixture class files are copied into a Gradle main-output layout and imported from there; the
 * {@code TestOutput*} fixtures alone are imported from where they compiled.
 */
class ConstrainedConfigurationPropertiesMustBeValidatedRuleTest {

    private static final String ID = "corral.spring.constrained-configuration-properties-must-be-validated";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** Matched by none of {@code TestScope}'s test-output patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

    private static final String TEST_OUTPUT_PREFIX = "TestOutput";

    private static final int FIXTURE_CLASS_FILES = 33;

    @TempDir
    static Path outputRoot;

    private static Path productionOutput;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        productionOutput = copyIntoProductionLayout(outputRoot.resolve("examples"),
                name -> !name.startsWith(TEST_OUTPUT_PREFIX));
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(classFileOf(TestOutputProperties.class)),
                Location.of(classFileOf(TestOutputConfiguration.class))));

        assertEquals(FIXTURE_CLASS_FILES, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return ConstrainedConfigurationPropertiesMustBeValidatedRule.DEFINITION.evaluate(classes)
                .getFailureReport().getDetails();
    }

    private static String classLine(Class<?> type) {
        return "Class <" + type.getName() + "> is bound by @ConfigurationProperties and declares Bean Validation"
                + " constraints but is not annotated @Validated in (" + type.getSimpleName() + ".java:0)";
    }

    /** A method line without its {@code in (File.java:N)} suffix, whose line number moves with the fixture. */
    private static String methodLine(Class<?> owner, String method, Class<?> bound) {
        return "Method <" + owner.getName() + "." + method + "()> binds <" + bound.getName()
                + "> with @ConfigurationProperties but neither is annotated @Validated";
    }

    @ParameterizedTest
    @ValueSource(classes = {PaymentProperties.class, JavaxProperties.class, SearchProperties.class,
            JavaxCascadeProperties.class, CatalogProperties.class, ScheduleProperties.class,
            SmsChannelProperties.class, RetryProperties.class, ConsistencyProperties.class,
            QuorumProperties.class, PoolProperties.class, LegacyCodeProperties.class})
    void flagsClass(Class<?> type) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.contains(classLine(type)), String.join("\n", violations));
    }

    @ParameterizedTest
    @ValueSource(classes = {SearchIndex.class, ChannelProperties.class, RetryPolicy.class, Datasource.class,
            ValidatedSettings.class, CacheProperties.class, TreeProperties.class, IgnoredMembersProperties.class,
            SettingsHolder.class, SettingsHolder.Section.class, ClientSettings.class, TlsClientSettings.class,
            TestOutputProperties.class})
    void ignoresClass(Class<?> type) {
        List<String> violations = violationsIn(examples);

        assertFalse(violations.stream().anyMatch(line -> line.startsWith("Class <" + type.getName() + ">")),
                String.join("\n", violations));
    }

    @Test
    void flagsOnlyTheUnvalidatedBindingMethods() {
        Set<String> methodLines = violationsIn(examples).stream()
                .filter(line -> line.startsWith("Method <"))
                .map(line -> line.substring(0, line.lastIndexOf(" in (")))
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                        methodLine(ClientConfiguration.class, "clientSettings", ClientSettings.class),
                        methodLine(TlsClientConfiguration.class, "clientSettings", TlsClientSettings.class)),
                methodLines);
    }

    @Test
    void reportsNothingElse() {
        List<String> violations = violationsIn(examples);

        assertEquals(14, violations.size(), String.join("\n", violations));
    }

    /** Without resolution every Validation API type is a stub carrying no annotations: only the package names it. */
    @ParameterizedTest
    @ValueSource(classes = {PaymentProperties.class, JavaxProperties.class, ScheduleProperties.class})
    void flagsABuiltInConstraintByPackageWhenItsTypeIsNotResolved(Class<?> type) {
        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
        try {
            List<String> violations = violationsIn(new ClassFileImporter().importPath(productionOutput));

            assertTrue(violations.contains(classLine(type)), String.join("\n", violations));
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    /** The counterpart of ignoring the {@code TestOutput*} fixtures: the same class files, in a production layout. */
    @Test
    void flagsTheTestOutputExamplesOnceInAProductionLayout() throws IOException {
        Path copy = copyIntoProductionLayout(outputRoot.resolve("test-output"),
                name -> name.startsWith(TEST_OUTPUT_PREFIX) || name.equals(ClientSettings.class.getSimpleName() + ".class"));

        List<String> violations = violationsIn(new ClassFileImporter().importPath(copy));

        assertEquals(2, violations.size(), String.join("\n", violations));
        assertTrue(violations.contains(classLine(TestOutputProperties.class)), String.join("\n", violations));
        assertTrue(violations.stream().anyMatch(line -> line.startsWith(
                        methodLine(TestOutputConfiguration.class, "reportSettings", ClientSettings.class))),
                String.join("\n", violations));
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, ConstrainedConfigurationPropertiesMustBeValidatedRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains(classLine(PaymentProperties.class)), debt);
            assertTrue(debt.contains(methodLine(ClientConfiguration.class, "clientSettings", ClientSettings.class)), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    private static Path copyIntoProductionLayout(Path root, Predicate<String> include) throws IOException {
        Path destination = root.resolve(PRODUCTION_LAYOUT)
                .resolve(PaymentProperties.class.getPackageName().replace('.', '/'));
        Files.createDirectories(destination);
        try (Stream<Path> classFiles = Files.list(fixturesDirectory())) {
            for (Path classFile : classFiles.filter(file -> include.test(file.getFileName().toString())).toList()) {
                Files.copy(classFile, destination.resolve(classFile.getFileName()));
            }
        }
        return root.resolve(PRODUCTION_LAYOUT);
    }

    private static Path fixturesDirectory() {
        try {
            return Path.of(classFileOf(PaymentProperties.class).toURI()).getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static URL classFileOf(Class<?> type) {
        URL classFile = type.getResource(type.getSimpleName() + ".class");
        assertNotNull(classFile, () -> "no compiled class file on the classpath for " + type.getName());
        return classFile;
    }
}
