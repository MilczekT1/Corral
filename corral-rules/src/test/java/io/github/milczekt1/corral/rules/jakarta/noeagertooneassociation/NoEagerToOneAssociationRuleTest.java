package io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation.fixtures.LegacyInvoice;
import io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation.fixtures.PurchaseOrder;
import io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation.fixtures.Subscription;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoEagerToOneAssociationRuleTest {

    private static final String ID = "corral.jakarta.no-eager-to-one-association";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importPackagesOf(PurchaseOrder.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations(JavaClasses classes) {
        return NoEagerToOneAssociationRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
    }

    private static String member(Class<?> owner, String name) {
        return "<" + owner.getName() + "." + name + ">";
    }

    private static void assertFlagged(Class<?> owner, String name) {
        assertFlagged(EXAMPLES, owner, name);
    }

    private static void assertFlagged(JavaClasses classes, Class<?> owner, String name) {
        List<String> violations = violations(classes);

        assertTrue(violations.stream().anyMatch(line -> line.contains(member(owner, name))),
                String.join("\n", violations));
    }

    private static void assertNotReported(Class<?> owner, String name) {
        assertNotReported(EXAMPLES, owner, name);
    }

    private static void assertNotReported(JavaClasses classes, Class<?> owner, String name) {
        String report = String.join("\n", violations(classes));

        assertFalse(report.contains(member(owner, name)), report);
    }

    @Test
    void flagsAManyToOneWithNoFetch() {
        assertFlagged(PurchaseOrder.class, "customer");
    }

    @Test
    void flagsAManyToOneThatSetsOnlyOptional() {
        assertFlagged(PurchaseOrder.class, "approver");
    }

    @Test
    void flagsAOneToOneDeclaredEager() {
        assertFlagged(PurchaseOrder.class, "billingAddress");
    }

    @Test
    void flagsALazyManyToOneFetchedByJoin() {
        assertFlagged(PurchaseOrder.class, "warehouse");
    }

    @Test
    void flagsAJavaxManyToOneWithNoFetch() {
        assertFlagged(LegacyInvoice.class, "customer");
    }

    @Test
    void flagsAJavaxOneToOneWithNoFetch() {
        assertFlagged(LegacyInvoice.class, "payer");
    }

    @Test
    void flagsAGetterDeclaredEager() {
        assertFlagged(Subscription.class, "getCustomer()");
    }

    @Test
    void ignoresAManyToOneDeclaredLazy() {
        assertNotReported(PurchaseOrder.class, "courier");
    }

    @Test
    void ignoresALazyManyToOneFetchedBySelect() {
        assertNotReported(PurchaseOrder.class, "merchant");
    }

    @Test
    void ignoresAOneToMany() {
        assertNotReported(PurchaseOrder.class, "deliveryAddresses");
    }

    @Test
    void ignoresAJavaxManyToOneDeclaredLazy() {
        assertNotReported(LegacyInvoice.class, "vendor");
    }

    @Test
    void ignoresAJavaxOneToOneDeclaredLazy() {
        assertNotReported(LegacyInvoice.class, "shippingAddress");
    }

    @Test
    void ignoresAGetterDeclaredLazy() {
        assertNotReported(Subscription.class, "getAddress()");
    }

    @Test
    void ignoresTheUnannotatedFieldsBehindMappedGetters() {
        assertNotReported(Subscription.class, "customer");
        assertNotReported(Subscription.class, "address");
    }

    @Test
    void reportsOneViolationPerFlaggedMember() {
        List<String> violations = violations(EXAMPLES);

        assertEquals(8, violations.size(), String.join("\n", violations));
        assertEquals(1, violations.stream().filter(line -> line.contains(member(PurchaseOrder.class, "reseller")))
                .count(), String.join("\n", violations));
    }

    @Test
    void reportsEveryCauseInOneShape() {
        List<String> violations = violations(EXAMPLES);

        assertTrue(violations.contains("Field " + member(PurchaseOrder.class, "customer")
                + " is a to-one association that may be fetched eagerly in (PurchaseOrder.java:0)"),
                String.join("\n", violations));
        assertTrue(violations.contains("Field " + member(PurchaseOrder.class, "warehouse")
                + " is a to-one association that may be fetched eagerly in (PurchaseOrder.java:0)"),
                String.join("\n", violations));
        assertTrue(violations.stream().anyMatch(line -> line.startsWith("Method "
                + member(Subscription.class, "getCustomer()")
                + " is a to-one association that may be fetched eagerly in (Subscription.java:")),
                String.join("\n", violations));
    }

    @Test
    void flagsAMissingFetchWhenTheAnnotationTypeDoesNotResolve() {
        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
        try {
            JavaClasses unresolved = new ClassFileImporter().importClasses(PurchaseOrder.class);

            assertTrue(unresolved.get(PurchaseOrder.class).getField("customer")
                            .getAnnotationOfType("jakarta.persistence.ManyToOne").get("fetch").isEmpty(),
                    "fetch must read absent, or this test proves nothing beyond the resolved case");
            List<String> violations = violations(unresolved);
            assertTrue(violations.contains("Field " + member(PurchaseOrder.class, "customer")
                    + " is a to-one association that may be fetched eagerly in (PurchaseOrder.java:0)"),
                    String.join("\n", violations));
            assertNotReported(unresolved, PurchaseOrder.class, "courier");
        } finally {
            ArchConfiguration.get().reset();
        }
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoEagerToOneAssociationRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains(member(PurchaseOrder.class, "customer")), debt);
            assertTrue(debt.contains(member(LegacyInvoice.class, "payer")), debt);
            assertTrue(debt.contains(member(Subscription.class, "getCustomer()")), debt);
            assertFalse(debt.contains(member(PurchaseOrder.class, "courier")), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
