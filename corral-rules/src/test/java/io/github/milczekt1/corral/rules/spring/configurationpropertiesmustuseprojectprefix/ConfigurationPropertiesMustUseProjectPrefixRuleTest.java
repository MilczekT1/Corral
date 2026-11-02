package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.TestOutputProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.AppProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.AuditProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.BillingProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.ChannelProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.ClientConfiguration;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.ClientSettings;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.ConfigurationTemplate;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.CorralProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.LegacyBinding;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.MailProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.PaymentProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.RootProperties;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.SettingsTemplate;
import io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix.fixtures.production.TimeoutDefaults;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
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
 */
class ConfigurationPropertiesMustUseProjectPrefixRuleTest {

    private static final String ID = "corral.spring.configuration-properties-must-use-project-prefix";

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = ProductionLayout.importFixtures(ConfigurationPropertiesMustUseProjectPrefixRuleTest.class, outputRoot.resolve("examples"));
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

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(ConfigurationPropertiesMustUseProjectPrefixRule.rule, ID, examples);

        assertTrue(debt.contains(classLine(AppProperties.class, "app")), debt);
        assertTrue(debt.contains(methodLine(ClientConfiguration.class, "clientSettings", "client")), debt);
    }
}
