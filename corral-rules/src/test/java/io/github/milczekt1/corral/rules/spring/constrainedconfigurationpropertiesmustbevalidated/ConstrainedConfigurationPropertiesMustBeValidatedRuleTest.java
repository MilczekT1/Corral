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
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ArchiveArrayProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.BaseChannelProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.CacheProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.CatalogProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ClientConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ClientSettings;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ConsistencyProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ConstantsProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ConstrainedProfile;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.CreateOrderRequest;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.CsvConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.CsvSettings;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.Datasource;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.EndpointProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ExportConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ExportSettings;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.FallbackOptionalProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.FeatureProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.GatewayConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.GatewaySettings;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.HostnameProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.JavaxConstraintProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.JavaxNotNullProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.JdkTypesProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.LabelProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.LegacyCascadeProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.MailProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.OrderProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.PaymentProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.PayoutProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.PoolNestingProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.PrimaryDbProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ProfileBase;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ProfileConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ProxyProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.QueueConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.QueueProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.QuorumProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.RateLimitProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.RegionProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ReplicaListProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ReportSettings;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ResolverProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.RetryProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ScheduleProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.SearchProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.SettingsHolder;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ShardMapProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.SmsChannelProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.StorageConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.StorageSettings;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.TagProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.TenantProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.TestOutputConfiguration;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.TestOutputProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.TreeProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.UsernameProperties;
import io.github.milczekt1.corral.rules.spring.constrainedconfigurationpropertiesmustbevalidated.fixtures.ValidatedOuterProperties;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    private static final String CLASS_SHAPE = " is bound by @ConfigurationProperties and declares Bean Validation"
            + " constraints but is not annotated @Validated in ";

    private static final int FIXTURE_CLASS_FILES = 71;

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

    private static void assertClassFlagged(Class<?> type) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.startsWith("Class <" + type.getName() + ">" + CLASS_SHAPE)),
                String.join("\n", violations));
    }

    private static void assertNoClassLine(Class<?> type) {
        List<String> violations = violationsIn(examples);

        assertFalse(violations.stream().anyMatch(line -> line.startsWith("Class <" + type.getName() + ">")),
                String.join("\n", violations));
    }

    private static void assertNoMethodLine(Class<?> owner) {
        List<String> violations = violationsIn(examples);

        assertFalse(violations.stream().anyMatch(line -> line.startsWith("Method <" + owner.getName() + ".")),
                String.join("\n", violations));
    }

    private static void assertNotReported(Class<?> type) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains("<" + type.getName() + ">") || report.contains("<" + type.getName() + "."), report);
    }

    @Test
    void flagsAConstrainedField() {
        assertClassFlagged(PaymentProperties.class);
    }

    @Test
    void flagsATypeLevelConstraint() {
        assertClassFlagged(ScheduleProperties.class);
    }

    @Test
    void flagsAConstrainedRecordComponent() {
        assertClassFlagged(MailProperties.class);
    }

    @Test
    void flagsAFieldCarryingOnlyValid() {
        assertClassFlagged(SearchProperties.class);
    }

    @Test
    void flagsAFieldCarryingOnlyJavaxValid() {
        assertClassFlagged(LegacyCascadeProperties.class);
    }

    @Test
    void flagsAProjectConstraintMetaAnnotatedConstraint() {
        assertClassFlagged(PayoutProperties.class);
    }

    @Test
    void flagsAProjectConstraintMetaAnnotatedJavaxConstraint() {
        assertClassFlagged(JavaxConstraintProperties.class);
    }

    /** Without resolution every Validation API type is a stub carrying no annotations: only the package names it. */
    @Test
    void flagsBuiltInConstraintsByPackageWhenTheirTypesAreNotResolved() {
        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
        try {
            List<String> violations = violationsIn(new ClassFileImporter().importPath(productionOutput));

            for (Class<?> type : List.of(PaymentProperties.class, JavaxNotNullProperties.class,
                    UsernameProperties.class, ScheduleProperties.class)) {
                assertTrue(violations.stream().anyMatch(line -> line.startsWith("Class <" + type.getName() + ">")),
                        type.getSimpleName() + " not flagged in:\n" + String.join("\n", violations));
            }
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    @Test
    void flagsARepeatedBuiltInConstraint() {
        assertClassFlagged(HostnameProperties.class);
    }

    @Test
    void flagsAJavaxConstraint() {
        assertClassFlagged(JavaxNotNullProperties.class);
    }

    @Test
    void flagsAConstrainedFieldInheritedFromAnUnannotatedBase() {
        assertClassFlagged(RegionProperties.class);
    }

    @Test
    void flagsABoundClassEvenWhenItsBindingMethodIsValidated() {
        assertClassFlagged(QueueProperties.class);
        assertNoMethodLine(QueueConfiguration.class);
    }

    @Test
    void flagsARepeatedHibernateValidatorConstraint() {
        assertClassFlagged(UsernameProperties.class);
    }

    @Test
    void flagsARepeatedProjectConstraint() {
        assertClassFlagged(CatalogProperties.class);
    }

    @Test
    void flagsAClassInheritingConfigurationProperties() {
        assertClassFlagged(TenantProperties.class);
    }

    @Test
    void flagsAConstraintOnlyInAHeldType() {
        assertClassFlagged(PrimaryDbProperties.class);
    }

    @Test
    void flagsAConstrainedListElement() {
        assertClassFlagged(ReplicaListProperties.class);
    }

    @Test
    void flagsAConstrainedMapValue() {
        assertClassFlagged(ShardMapProperties.class);
    }

    @Test
    void flagsAConstrainedOptionalContent() {
        assertClassFlagged(FallbackOptionalProperties.class);
    }

    @Test
    void flagsAConstrainedArrayComponent() {
        assertClassFlagged(ArchiveArrayProperties.class);
    }

    @Test
    void flagsAConstrainedTypeNestedInTwoTypeArguments() {
        assertClassFlagged(PoolNestingProperties.class);
    }

    @Test
    void flagsAConstrainedInterfaceGetter() {
        assertClassFlagged(RetryProperties.class);
    }

    @Test
    void flagsAConstrainedIsGetter() {
        assertClassFlagged(ConsistencyProperties.class);
    }

    @Test
    void flagsAConstrainedHasGetter() {
        assertClassFlagged(QuorumProperties.class);
    }

    @Test
    void flagsAConcreteSubclassOfAConstrainedAbstractBase() {
        assertClassFlagged(SmsChannelProperties.class);
        assertNoClassLine(BaseChannelProperties.class);
    }

    @Test
    void flagsAnUnvalidatedBindingMethodButNotTheClassItBinds() {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.startsWith(
                        "Method <" + ClientConfiguration.class.getName() + ".clientSettings()> binds <"
                                + ClientSettings.class.getName() + "> with @ConfigurationProperties")),
                String.join("\n", violations));
        assertNoClassLine(ClientSettings.class);
    }

    /** javac copies the override's annotations onto its synthetic bridge, which returns the supertype. */
    @Test
    void flagsACovariantBindingMethodOverrideWithoutItsBridge() {
        List<String> violations = violationsIn(examples);
        String override = "Method <" + CsvConfiguration.class.getName() + ".exportSettings()> binds <";

        assertTrue(violations.stream().anyMatch(line -> line.startsWith(override + CsvSettings.class.getName() + ">")),
                String.join("\n", violations));
        assertTrue(violations.stream().anyMatch(line -> line.startsWith(
                        "Method <" + ExportConfiguration.class.getName() + ".exportSettings()> binds <"
                                + ExportSettings.class.getName() + ">")),
                String.join("\n", violations));
        assertFalse(violations.stream().anyMatch(line -> line.startsWith(override + ExportSettings.class.getName() + ">")),
                String.join("\n", violations));
    }

    @Test
    void reportsOneViolationPerBoundClassOrBindingMethod() {
        List<String> violations = violationsIn(examples);

        assertEquals(27, violations.size(), String.join("\n", violations));
        assertEquals(violations.size(), violations.stream().distinct().count(), String.join("\n", violations));
    }

    @Test
    void reportsAClassAndABindingMethodInOneShapeEach() {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.contains("Class <" + PaymentProperties.class.getName() + ">" + CLASS_SHAPE
                        + "(PaymentProperties.java:0)"),
                String.join("\n", violations));
        assertTrue(violations.stream().anyMatch(line -> line.matches("\\QMethod <" + ClientConfiguration.class.getName()
                        + ".clientSettings()> binds <" + ClientSettings.class.getName()
                        + "> with @ConfigurationProperties but neither is annotated @Validated in"
                        + " (ClientConfiguration.java:\\E\\d+\\)")),
                String.join("\n", violations));
    }

    @Test
    void ignoresAValidatedClass() {
        assertNotReported(OrderProperties.class);
    }

    @Test
    void ignoresAValidatedClassHoldingAConstrainedType() {
        assertNotReported(ValidatedOuterProperties.class);
    }

    @Test
    void ignoresAClassHoldingOnlyJdkTypes() {
        assertNotReported(JdkTypesProperties.class);
    }

    @Test
    void ignoresAnUnconstrainedSelfReferencingHeldType() {
        assertNotReported(TreeProperties.class);
    }

    @Test
    void ignoresAClassWithAValidatedSuperclass() {
        assertNotReported(CacheProperties.class);
    }

    @Test
    void ignoresAClassValidatedThroughAComposedAnnotation() {
        assertNotReported(RateLimitProperties.class);
    }

    @Test
    void ignoresAnUnconstrainedClass() {
        assertNotReported(FeatureProperties.class);
    }

    @Test
    void ignoresAConstrainedClassThatIsNotBound() {
        assertNotReported(CreateOrderRequest.class);
        assertNotReported(Datasource.class);
    }

    @Test
    void ignoresAFactoryMethodWithoutConfigurationProperties() {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains(ClientConfiguration.class.getName() + ".fallbackClientSettings()"), report);
    }

    @Test
    void ignoresAnAnnotationThatCarriesAConstraintWithoutBeingOne() {
        assertNotReported(LabelProperties.class);
    }

    @Test
    void ignoresAValidatedBindingMethod() {
        assertNotReported(GatewaySettings.class);
        assertNoMethodLine(GatewayConfiguration.class);
    }

    @Test
    void ignoresABindingMethodDeclaredToReturnASupertype() {
        assertNotReported(ProfileBase.class);
        assertNotReported(ConstrainedProfile.class);
        assertNoMethodLine(ProfileConfiguration.class);
    }

    @Test
    void ignoresAnUnvalidatedBindingMethodOfAValidatedClass() {
        assertNotReported(StorageSettings.class);
        assertNoMethodLine(StorageConfiguration.class);
    }

    @Test
    void ignoresAConstructorParameterConstraint() {
        assertNotReported(EndpointProperties.class);
    }

    @Test
    void ignoresASetterParameterConstraint() {
        assertNotReported(ProxyProperties.class);
    }

    @Test
    void ignoresATypeArgumentConstraint() {
        assertNotReported(TagProperties.class);
    }

    @Test
    void ignoresConstrainedMethodsThatAreNotGetters() {
        assertNotReported(ResolverProperties.class);
    }

    @Test
    void ignoresStaticFieldsAndGetters() {
        assertNotReported(ConstantsProperties.class);
    }

    @Test
    void ignoresTheSyntheticOuterReferenceOfAnInnerClass() {
        assertNotReported(SettingsHolder.Section.class);
        assertNotReported(SettingsHolder.class);
    }

    @Test
    void ignoresClassesAndBindingMethodsInTestOutput() {
        assertNotReported(TestOutputProperties.class);
        assertNotReported(ReportSettings.class);
        assertNoMethodLine(TestOutputConfiguration.class);
    }

    /** The counterpart of the test-output case: the same class files, copied into a production layout. */
    @Test
    void flagsTheTestOutputFixturesOnceInAProductionLayout() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("test-output"),
                name -> name.startsWith(TEST_OUTPUT_PREFIX)
                        || name.equals(ReportSettings.class.getSimpleName() + ".class"));
        JavaClasses copied = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(copied);

        assertEquals(2, violations.size(), String.join("\n", violations));
        assertTrue(violations.stream().anyMatch(line -> line.startsWith("Class <" + TestOutputProperties.class.getName() + ">")),
                String.join("\n", violations));
        assertTrue(violations.stream().anyMatch(line -> line.startsWith("Method <" + TestOutputConfiguration.class.getName() + ".")),
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
            assertTrue(debt.contains("Class <" + PaymentProperties.class.getName() + ">"), debt);
            assertTrue(debt.contains("Class <" + PoolNestingProperties.class.getName() + ">"), debt);
            assertTrue(debt.contains("Method <" + ClientConfiguration.class.getName() + ".clientSettings()>"), debt);
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
