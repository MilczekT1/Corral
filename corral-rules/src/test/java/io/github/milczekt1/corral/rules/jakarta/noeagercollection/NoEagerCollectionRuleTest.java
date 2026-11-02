package io.github.milczekt1.corral.rules.jakarta.noeagercollection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.jakarta.noeagercollection.fixtures.LegacyCart;
import io.github.milczekt1.corral.rules.jakarta.noeagercollection.fixtures.PropertyAccessCart;
import io.github.milczekt1.corral.rules.jakarta.noeagercollection.fixtures.ShoppingCart;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoEagerCollectionRuleTest {

    private static final String ID = "corral.jakarta.no-eager-collection";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importPackagesOf(ShoppingCart.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations(JavaClasses classes) {
        return NoEagerCollectionRule.DEFINITION.evaluate(classes).getFailureReport().getDetails();
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
    void flagsAOneToManyDeclaredEager() {
        assertFlagged(ShoppingCart.class, "items");
    }

    @Test
    void flagsAManyToManyDeclaredEager() {
        assertFlagged(ShoppingCart.class, "coupons");
    }

    @Test
    void flagsAnElementCollectionDeclaredEager() {
        assertFlagged(ShoppingCart.class, "labels");
    }

    @Test
    void flagsALazyOneToManyFetchedByJoin() {
        assertFlagged(ShoppingCart.class, "shipments");
    }

    @Test
    void flagsAJavaxOneToManyDeclaredEager() {
        assertFlagged(LegacyCart.class, "items");
    }

    @Test
    void flagsAJavaxManyToManyDeclaredEager() {
        assertFlagged(LegacyCart.class, "wishlist");
    }

    @Test
    void flagsAJavaxElementCollectionDeclaredEager() {
        assertFlagged(LegacyCart.class, "labels");
    }

    @Test
    void flagsAGetterDeclaredEager() {
        assertFlagged(PropertyAccessCart.class, "getItems()");
    }

    @Test
    void ignoresAOneToManyWithNoFetch() {
        assertNotReported(ShoppingCart.class, "refunds");
    }

    @Test
    void ignoresAnElementCollectionWithNoFetch() {
        assertNotReported(ShoppingCart.class, "notes");
    }

    @Test
    void ignoresAManyToManyDeclaredLazy() {
        assertNotReported(ShoppingCart.class, "tags");
    }

    @Test
    void ignoresALazyOneToManyFetchedBySubselect() {
        assertNotReported(ShoppingCart.class, "allocations");
    }

    @Test
    void ignoresAnEagerManyToOne() {
        assertNotReported(ShoppingCart.class, "owner");
    }

    @Test
    void ignoresAManyToOneFetchedByJoin() {
        assertNotReported(ShoppingCart.class, "referrer");
    }

    @Test
    void ignoresAJavaxOneToManyWithNoFetch() {
        assertNotReported(LegacyCart.class, "returns");
    }

    @Test
    void ignoresAJavaxElementCollectionDeclaredLazy() {
        assertNotReported(LegacyCart.class, "notes");
    }

    @Test
    void ignoresAGetterDeclaredLazy() {
        assertNotReported(PropertyAccessCart.class, "getTags()");
    }

    @Test
    void ignoresTheUnannotatedFieldsBehindMappedGetters() {
        assertNotReported(PropertyAccessCart.class, "items");
        assertNotReported(PropertyAccessCart.class, "tags");
    }

    @Test
    void reportsOneViolationPerFlaggedMember() {
        List<String> violations = violations(EXAMPLES);

        assertEquals(9, violations.size(), String.join("\n", violations));
        assertEquals(1, violations.stream().filter(line -> line.contains(member(ShoppingCart.class, "vouchers")))
                .count(), String.join("\n", violations));
    }

    @Test
    void reportsEveryCauseInOneShape() {
        List<String> violations = violations(EXAMPLES);

        assertTrue(violations.contains("Field " + member(ShoppingCart.class, "items")
                + " is a collection mapping fetched eagerly in (ShoppingCart.java:0)"),
                String.join("\n", violations));
        assertTrue(violations.contains("Field " + member(ShoppingCart.class, "shipments")
                + " is a collection mapping fetched eagerly in (ShoppingCart.java:0)"),
                String.join("\n", violations));
        assertTrue(violations.stream().anyMatch(line -> line.startsWith("Method "
                + member(PropertyAccessCart.class, "getItems()")
                + " is a collection mapping fetched eagerly in (PropertyAccessCart.java:")),
                String.join("\n", violations));
    }

    @Test
    void readsFetchWhenTheAnnotationTypeDoesNotResolve() {
        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
        try {
            JavaClasses unresolved = new ClassFileImporter().importClasses(ShoppingCart.class);

            assertTrue(unresolved.get(ShoppingCart.class).getField("refunds")
                            .getAnnotationOfType("jakarta.persistence.OneToMany").get("fetch").isEmpty(),
                    "fetch must read absent, or this test proves nothing beyond the resolved case");
            assertFlagged(unresolved, ShoppingCart.class, "items");
            assertFlagged(unresolved, ShoppingCart.class, "shipments");
            assertNotReported(unresolved, ShoppingCart.class, "refunds");
        } finally {
            ArchConfiguration.get().reset();
        }
    }

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(NoEagerCollectionRuleTest.class, NoEagerCollectionRule.DEFINITION, EXAMPLES);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoEagerCollectionRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains(member(ShoppingCart.class, "items")), debt);
        assertTrue(debt.contains(member(LegacyCart.class, "wishlist")), debt);
        assertTrue(debt.contains(member(PropertyAccessCart.class, "getItems()")), debt);
        assertFalse(debt.contains(member(ShoppingCart.class, "refunds")), debt);
    }
}
