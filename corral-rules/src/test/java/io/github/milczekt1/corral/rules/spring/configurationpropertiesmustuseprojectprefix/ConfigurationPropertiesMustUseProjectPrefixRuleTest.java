package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.AppProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.AuditProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.BillingProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.ChannelProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.ClientConfiguration;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.ClientSettings;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.ConfigurationTemplate;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.CorralProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.LegacyBinding;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.MailProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.PaymentProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.RootProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.SettingsTemplate;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.TestOutputProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.TimeoutDefaults;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Reads {@code corral.property-prefix=corral} from this module's test {@code corral.properties}.
 *
 * <p>The rule is scoped to production classes, and every fixture compiles into test output. So the
 * fixture class files are copied into a Gradle main-output layout and imported from there;
 * {@link TestOutputProperties} alone is imported from where it compiled.
 */
class ConfigurationPropertiesMustUseProjectPrefixRuleTest {

    private static final String ID = "corral.spring.configuration-properties-must-use-project-prefix";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final int FIXTURE_CLASS_FILES = 15;

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = ProductionLayout.copyPackageOf(AppProperties.class, outputRoot,
                name -> !name.startsWith(TestOutputProperties.class.getSimpleName()));
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(ProductionLayout.classFileOf(TestOutputProperties.class))));

        assertEquals(FIXTURE_CLASS_FILES, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return ConfigurationPropertiesMustUseProjectPrefixRule.DEFINITION.evaluate(classes)
                .getFailureReport().getDetails();
    }

    private static String classLine(Class<?> type, String prefix) {
        return "Class <" + type.getName() + "> declares @ConfigurationProperties prefix \"" + prefix
                + "\" outside the configured property prefix in (" + type.getSimpleName() + ".java:0)";
    }

    /** A method line without its {@code in (File.java:N)} suffix, whose line number moves with the fixture. */
    private static String methodLine(Class<?> owner, String method, String prefix) {
        return "Method <" + owner.getName() + "." + method + "()> declares @ConfigurationProperties prefix \""
                + prefix + "\" outside the configured property prefix";
    }

    static Stream<Arguments> flaggedClasses() {
        return Stream.of(
                Arguments.of(AppProperties.class, "app"),
                Arguments.of(AuditProperties.class, "server.audit"),
                Arguments.of(RootProperties.class, ""),
                Arguments.of(MailProperties.class, "corralx.mail"),
                Arguments.of(ChannelProperties.class, "channel"),
                Arguments.of(LegacyBinding.class, "legacy"));
    }

    @ParameterizedTest
    @MethodSource("flaggedClasses")
    void flagsClass(Class<?> type, String prefix) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.contains(classLine(type, prefix)), String.join("\n", violations));
    }

    @ParameterizedTest
    @ValueSource(classes = {PaymentProperties.class, BillingProperties.class, CorralProperties.class,
            TimeoutDefaults.class, ClientSettings.class, SettingsTemplate.class, ConfigurationTemplate.class,
            ClientConfiguration.class, TestOutputProperties.class})
    void ignoresClass(Class<?> type) {
        List<String> violations = violationsIn(examples);

        assertFalse(violations.stream().anyMatch(line -> line.startsWith("Class <" + type.getName() + ">")),
                String.join("\n", violations));
    }

    /**
     * Not {@code hikariProperties()}, whose return type is not imported, not {@code regionalClientSettings()},
     * under the project prefix, and not the bridge javac generates.
     */
    @Test
    void flagsTheBindingMethodReturningAnImportedClassOnce() {
        List<String> methodLines = violationsIn(examples).stream()
                .filter(line -> line.startsWith("Method <"))
                .map(line -> line.substring(0, line.lastIndexOf(" in (")))
                .toList();

        assertEquals(List.of(methodLine(ClientConfiguration.class, "clientSettings", "client")), methodLines);
    }

    @Test
    void reportsNothingElse() {
        List<String> violations = violationsIn(examples);

        assertEquals(7, violations.size(), String.join("\n", violations));
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, ConfigurationPropertiesMustUseProjectPrefixRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains(classLine(AppProperties.class, "app")), debt);
            assertTrue(debt.contains(methodLine(ClientConfiguration.class, "clientSettings", "client")), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
