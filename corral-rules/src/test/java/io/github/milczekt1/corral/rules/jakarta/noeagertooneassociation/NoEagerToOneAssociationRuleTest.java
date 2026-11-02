package io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation.fixtures.LegacyInvoice;
import io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation.fixtures.PurchaseOrder;
import io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation.fixtures.Subscription;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoEagerToOneAssociationRuleTest {

    private static final String ID = "corral.jakarta.no-eager-to-one-association";

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

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoEagerToOneAssociationRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains(member(PurchaseOrder.class, "customer")), debt);
        assertTrue(debt.contains(member(LegacyInvoice.class, "payer")), debt);
        assertTrue(debt.contains(member(Subscription.class, "getCustomer()")), debt);
        assertFalse(debt.contains(member(PurchaseOrder.class, "courier")), debt);
    }
}
