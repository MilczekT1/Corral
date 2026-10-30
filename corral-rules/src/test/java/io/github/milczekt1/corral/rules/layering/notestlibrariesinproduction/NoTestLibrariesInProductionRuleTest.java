package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures.OrderService;
import io.github.milczekt1.corral.rules.testsupport.ProductionLayout;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The rule is scoped to production classes, and every fixture compiles into test output. So the
 * fixture class files are copied into Gradle output layouts and imported from there.
 */
class NoTestLibrariesInProductionRuleTest {

    private static final String ID = "corral.layering.no-test-libraries-in-production";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final String FIXTURES = OrderService.class.getPackageName() + ".";

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        examples = importFromLayout(ProductionLayout.MAIN_OUTPUT);

        assertEquals(fixtureClassFiles().size(), examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoTestLibrariesInProductionRule.DEFINITION
                .allowEmptyShould(true).evaluate(classes).getFailureReport().getDetails();
    }

    private static String report() {
        return String.join("\n", violationsIn(examples));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "OrderService, org.junit.jupiter.api.Assertions",
            "LegacyInvoiceValidator, junit.framework.AssertionFailedError",
            "ReconciliationCheck, org.opentest4j.TestAbortedException",
            "CurrencyConverter, org.testng.Assert",
            "EventRelay, spock.util.concurrent.BlockingVariable",
            "ShippingQuote, org.spockframework.util.Assert",
            "DiscountPolicy, io.kotest.matchers.ShouldKt",
            "NotificationFallback, org.mockito.Mockito",
            "ConfigInspector, org.powermock.reflect.Whitebox",
            "ClockOverride$1, mockit.MockUp",
            "AuditTrailStub, org.easymock.EasyMock",
            "SchedulerBridge, io.mockk.MockKException",
            "PriceCalculator, org.assertj.core.util.VisibleForTesting",
            "InventoryGuard, org.hamcrest.MatcherAssert",
            "TaxRateTable, com.google.common.truth.Truth",
            "LocalDatabaseBootstrap, org.testcontainers.containers.GenericContainer",
            "CacheWarmer, org.springframework.test.util.ReflectionTestUtils",
            "ReportUploader, org.springframework.mock.web.MockMultipartFile",
            "SystemUserRunner, org.springframework.security.test.context.TestSecurityContextHolder",
            "PropertyOverrides, org.springframework.boot.test.util.TestPropertyValues",
            "ControllerSmokeCheck, org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest",
            "PartnerApiClient, org.springframework.boot.resttestclient.TestRestTemplate",
            "StorefrontRehearsal, org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient",
            "RetrySimulator, reactor.test.scheduler.VirtualTimeScheduler",
            "PaymentStatusPoller, org.awaitility.Awaitility",
            "HealthProbeClient, io.restassured.RestAssured"})
    void flagsADependencyOnEachTestLibrary(String subject, String target) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains(FIXTURES + subject) && line.contains("<" + target)),
                String.join("\n", violations));
    }

    @Test
    void flagsAJUnitTestClassCompiledIntoProductionOutput() {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains(FIXTURES + "ShippedCheckoutTest.totalsTheBasket()")
                        && line.contains("<org.junit.jupiter.api.Test>")),
                String.join("\n", violations));
    }

    @Test
    void ignoresProductionDependenciesOfAFlaggedClass() {
        String report = report();

        assertFalse(report.contains("<java."), report);
        assertFalse(report.contains("<org.springframework.security.authentication."), report);
        assertFalse(report.contains("<org.springframework.context."), report);
    }

    @Test
    void ignoresProductionPreconditionApis() {
        String report = report();

        assertFalse(report.contains(FIXTURES + "PaymentService"), report);
    }

    @Test
    void ignoresGuavasVisibleForTesting() {
        String report = report();

        assertFalse(report.contains(FIXTURES + "StockLevelCache"), report);
    }

    @Test
    void ignoresAnOrdinaryProductionLibrary() {
        String report = report();

        assertFalse(report.contains(FIXTURES + "RefundProcessor"), report);
    }

    @Test
    void ignoresSpringBootsProductionPackages() {
        String report = report();

        assertFalse(report.contains(FIXTURES + "ApplicationLauncher"), report);
    }

    @Test
    void ignoresTheExamplesWhereTheyCompiledInMavenTestOutput() {
        JavaClasses testOutput = new ClassFileImporter().importPath(ProductionLayout.directoryOf(OrderService.class));

        assertEquals(List.of(), violationsIn(testOutput));
    }

    @ParameterizedTest
    @ValueSource(strings = {"build/classes/java/testFixtures", "build/classes/kotlin/integrationTest",
            "build/classes/kotlin/jvm/test"})
    void ignoresTheExamplesInAGradleTestSourceSet(String layout) throws IOException {
        assertEquals(List.of(), violationsIn(importFromLayout(layout)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"build/classes/kotlin/jvm/main", "build/classes/java/api"})
    void flagsEveryExampleInAGradleProductionSourceSetNotNamedMain(String layout) throws IOException {
        assertEquals(Set.copyOf(violationsIn(examples)), Set.copyOf(violationsIn(importFromLayout(layout))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:/repo/target/test-classes/com/acme/Fixture.class",
            "file:/repo/out/test/classes/com/acme/Fixture.class",
            "file:/repo/build/classes/test/com/acme/LegacyGradleLayout.class",
            "file:/repo/build/classes/java/test/com/acme/ServiceTest.class",
            "file:/repo/build/classes/java/testFixtures/com/acme/Builder.class",
            "file:/repo/build/classes/kotlin/integrationTest/com/acme/PostgresBase.class",
            "file:/repo/build/classes/kotlin/jvm/test/com/acme/Spec.class",
            "file:/repo/build/classes/java/contractTest/com/acme/Contract.class"})
    void recognisesTestOutput(String uri) {
        assertTrue(NoTestLibrariesInProductionRule.isTestOutput(uri));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:/repo/build/classes/java/main/com/acme/Service.class",
            "file:/repo/build/classes/kotlin/jvm/main/com/acme/Service.class",
            "file:/repo/build/classes/java/api/com/acme/Service.class",
            "file:/repo/build/classes/java/main/latest/Service.class",
            "file:/repo/build/classes/kotlin/main/latest/Service.class",
            "file:/repo/build/classes/java/api/latest/Service.class",
            "file:/repo/build/classes/java/jmh/contest/Benchmark.class",
            "file:/repo/target/classes/com/acme/Service.class",
            "file:/repo/out/production/classes/com/acme/Service.class"})
    void doesNotMistakeProductionOutputForTestOutput(String uri) {
        assertFalse(NoTestLibrariesInProductionRule.isTestOutput(uri));
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoTestLibrariesInProductionRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains(FIXTURES + "OrderService.place(java.lang.String)> calls method"
                    + " <org.junit.jupiter.api.Assertions.assertNotNull(java.lang.Object)> in (OrderService.java:"), debt);
            assertTrue(debt.contains(FIXTURES + "PriceCalculator.round(java.math.BigDecimal)> is annotated with"
                    + " <org.assertj.core.util.VisibleForTesting> in (PriceCalculator.java:"), debt);
            assertFalse(debt.contains("PaymentService"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    /** Copies every fixture class file into {@code outputRoot/layout}, keeping package directories. */
    private static JavaClasses importFromLayout(String layout) throws IOException {
        Path root = outputRoot.resolve(layout.replace('/', '-')).resolve(layout);
        Path destination = root.resolve(OrderService.class.getPackageName().replace('.', '/'));
        Files.createDirectories(destination);
        for (Path classFile : fixtureClassFiles()) {
            Files.copy(classFile, destination.resolve(classFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
        return new ClassFileImporter().importPath(root);
    }

    private static List<Path> fixtureClassFiles() throws IOException {
        try (Stream<Path> files = Files.list(ProductionLayout.directoryOf(OrderService.class))) {
            return files.filter(file -> file.toString().endsWith(".class")).toList();
        }
    }
}
