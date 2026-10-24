package io.github.milczekt1.corral.rules.jackson.noannotationsonentity;

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
import io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures.PurchaseOrder;
import io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures.TestOutputTicket;
import io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures.WireName;
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
 * fixture class files are copied into a Gradle main-output layout and imported from there;
 * {@link TestOutputTicket} alone is imported from where it compiled.
 */
class NoAnnotationsOnEntityRuleTest {

    private static final String ID = "corral.jackson.no-annotations-on-entity";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /** Matched by none of {@code TestScope}'s test-output patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

    private static final String ANNOTATED_WITH = "> is annotated with <";

    private static final int FIXTURE_CLASS_FILES = 14;

    @TempDir
    static Path outputRoot;

    private static JavaClasses examples;

    @BeforeAll
    static void importExamples() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("examples"),
                name -> !name.startsWith(TestOutputTicket.class.getSimpleName()));
        examples = new ClassFileImporter().importLocations(List.of(
                Location.of(productionOutput),
                Location.of(classFileOf(TestOutputTicket.class))));

        assertEquals(FIXTURE_CLASS_FILES, examples.size(),
                "every example must be imported, or the ignore assertions below are vacuous");
    }

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violationsIn(JavaClasses classes) {
        return NoAnnotationsOnEntityRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static void assertFlagged(String element, String annotation) {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains("." + element + ANNOTATED_WITH + annotation + ">")),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains("." + subject), report);
    }

    @Test
    void flagsAJacksonAnnotationOnAJakartaEntityClass() {
        assertFlagged("PurchaseOrder", "com.fasterxml.jackson.annotation.JsonIgnoreProperties");
    }

    @Test
    void flagsAJacksonAnnotationOnAnAssociationField() {
        assertFlagged("PurchaseOrder.customer", "com.fasterxml.jackson.annotation.JsonIgnore");
    }

    @Test
    void flagsAJakartaMappedSuperclass() {
        assertFlagged("AuditedBase.createdAt", "com.fasterxml.jackson.annotation.JsonProperty");
    }

    @Test
    void flagsAJavaxEntity() {
        assertFlagged("LegacyInvoice.issuedOn", "com.fasterxml.jackson.annotation.JsonFormat");
    }

    @Test
    void flagsAJavaxMappedSuperclassGetter() {
        assertFlagged("LegacyVersioned.getVersion()", "com.fasterxml.jackson.annotation.JsonIgnore");
    }

    @Test
    void flagsAJackson3DatabindAnnotation() {
        assertFlagged("Shipment.trackingNumber", "tools.jackson.databind.annotation.JsonSerialize");
    }

    @Test
    void flagsAJacksonModuleAnnotation() {
        assertFlagged("Parcel.weight", "com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty");
    }

    @Test
    void flagsAProjectAnnotationBundlingJacksonAnnotations() {
        assertFlagged("Coupon.code", WireName.class.getName());
    }

    @Test
    void flagsAConstructorParameter() {
        List<String> violations = violationsIn(examples);

        assertTrue(violations.stream().anyMatch(line -> line.contains(".Ledger.<init>(java.lang.Long)")
                        && line.contains("com.fasterxml.jackson.annotation.JsonProperty")),
                String.join("\n", violations));
    }

    /** Annotation members of enum or class type are dependencies too; each annotated element counts once. */
    @Test
    void reportsOneViolationPerAnnotationUse() {
        List<String> violations = violationsIn(examples);

        assertEquals(9, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresJacksonTypesThatAreNotAnnotations() {
        String report = String.join("\n", violationsIn(examples));

        assertFalse(report.contains("JsonNode"), report);
        assertFalse(report.contains("ObjectMapper"), report);
        assertFalse(report.contains("ToStringSerializer"), report);
    }

    @Test
    void ignoresAnEntityWithoutJacksonAnnotations() {
        assertNotReported("Customer");
    }

    @Test
    void ignoresAJsonbColumn() {
        assertNotReported("Settlement");
    }

    @Test
    void ignoresAResponseRecord() {
        assertNotReported("PayoutResponse");
    }

    @Test
    void ignoresAnEmbeddable() {
        assertNotReported("PostalAddress");
    }

    @Test
    void ignoresTheComposedAnnotationsDeclaration() {
        assertNotReported("WireName" + ANNOTATED_WITH);
    }

    @Test
    void ignoresAnEntityInTestOutput() {
        assertNotReported("TestOutputTicket");
    }

    @Test
    void flagsTheTestOutputEntityOnceItSitsInProductionOutput() throws IOException {
        Path productionOutput = copyIntoProductionLayout(outputRoot.resolve("ticket"),
                name -> name.equals(TestOutputTicket.class.getSimpleName() + ".class"));
        JavaClasses ticket = new ClassFileImporter().importPath(productionOutput);

        List<String> violations = violationsIn(ticket);

        assertTrue(violations.stream().anyMatch(line -> line.contains(".TestOutputTicket.secret" + ANNOTATED_WITH)),
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoAnnotationsOnEntityRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(examples);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains(".PurchaseOrder.customer" + ANNOTATED_WITH), debt);
            assertTrue(debt.contains(".Shipment.trackingNumber" + ANNOTATED_WITH), debt);
            assertTrue(debt.contains(".Coupon.code" + ANNOTATED_WITH), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    private static Path copyIntoProductionLayout(Path root, Predicate<String> include) throws IOException {
        Path destination = root.resolve(PRODUCTION_LAYOUT)
                .resolve(PurchaseOrder.class.getPackageName().replace('.', '/'));
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
            return Path.of(classFileOf(PurchaseOrder.class).toURI()).getParent();
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
