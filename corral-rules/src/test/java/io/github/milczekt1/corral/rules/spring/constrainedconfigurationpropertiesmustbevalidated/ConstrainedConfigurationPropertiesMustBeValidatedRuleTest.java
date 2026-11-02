package io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.TestOutputConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.TestOutputProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.CacheProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.CatalogProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.ChannelProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.ClientConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.ClientSettings;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.ConsistencyProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.Datasource;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.IgnoredMembersProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.JavaxCascadeProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.JavaxProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.LegacyCodeProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.PaymentProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.PoolProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.QuorumProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.RetryPolicy;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.RetryProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.ScheduleProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.SearchIndex;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.SearchProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.SettingsHolder;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.SmsChannelProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.TlsClientConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.TlsClientSettings;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.TreeProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.production.ValidatedSettings;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConstrainedConfigurationPropertiesMustBeValidatedRuleTest {

    private static final String ID = "corral.spring.constrained-configuration-properties-must-be-validated";

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = ProductionLayout.importFixtures(ConstrainedConfigurationPropertiesMustBeValidatedRuleTest.class,
                outputRoot.resolve("examples"));
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
    void flagsABuiltInConstraintByPackageWhenItsTypeIsNotResolved(Class<?> type) throws IOException {
        Path productionOutput = ProductionLayout.copyPackageOf(PaymentProperties.class, outputRoot.resolve("unresolved"));
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
        Path copy = ProductionLayout.copy(outputRoot.resolve("test-output"),
                List.of(TestOutputProperties.class, TestOutputConfiguration.class, ClientSettings.class));

        List<String> violations = violationsIn(new ClassFileImporter().importPath(copy));

        assertEquals(2, violations.size(), String.join("\n", violations));
        assertTrue(violations.contains(classLine(TestOutputProperties.class)), String.join("\n", violations));
        assertTrue(violations.stream().anyMatch(line -> line.startsWith(
                        methodLine(TestOutputConfiguration.class, "reportSettings", ClientSettings.class))),
                String.join("\n", violations));
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(ConstrainedConfigurationPropertiesMustBeValidatedRule.rule, ID, examples);

        assertTrue(debt.contains(classLine(PaymentProperties.class)), debt);
        assertTrue(debt.contains(methodLine(ClientConfiguration.class, "clientSettings", ClientSettings.class)), debt);
    }
}
