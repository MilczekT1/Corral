package io.github.milczekt1.corral.rules.java.noequalswithouthashcode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.AbstractValue;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.Amount;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.Barcode;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.BaseEntity;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.Coordinates;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.Customer;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.EqualityContract;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.FieldError;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.IdentityRef;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.IdentityToken;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.KeyedValue;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.Money;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.Node;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.ProductCode;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.RouteList;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.SeededHash;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.Sku;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.TagSet;
import io.github.milczekt1.corral.rules.java.noequalswithouthashcode.fixtures.ValidationFailure;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoEqualsWithoutHashCodeRuleTest {

    private static final String ID = "corral.java.no-equals-without-hashcode";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            Money.class, ProductCode.class, ValidationFailure.class, RouteList.class, IdentityToken.class,
            Sku.class, BaseEntity.class, Customer.class, Node.class, Amount.class, Coordinates.class,
            TagSet.class, EqualityContract.class, AbstractValue.class, KeyedValue.class, IdentityRef.class,
            FieldError.class, Barcode.class, SeededHash.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoEqualsWithoutHashCodeRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();
    }

    private static void assertFlagged(String simpleName) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains("fixtures." + simpleName + ">")),
                String.join("\n", violations));
    }

    private static void assertNotReported(String simpleName) {
        String report = String.join("\n", violations());

        assertFalse(report.contains("fixtures." + simpleName + ">"), report);
    }

    @Test
    void flagsEqualsWithoutHashCode() {
        assertFlagged("Money");
    }

    @Test
    void flagsEqualsBesideAHashCodeOverload() {
        assertFlagged("ProductCode");
    }

    @Test
    void flagsEqualsBesideAHashCodeOverloadInASuperclass() {
        assertFlagged("Barcode");
    }

    @Test
    void flagsEqualsWhenNoJdkSuperclassDeclaresHashCode() {
        assertFlagged("ValidationFailure");
    }

    @Test
    void flagsEqualsWhenOnlyAnInterfaceDeclaresHashCode() {
        assertFlagged("RouteList");
    }

    @Test
    void flagsAnIdentityEqualsWithoutHashCode() {
        assertFlagged("IdentityToken");
    }

    @Test
    void reportsExactlySixClasses() {
        assertEquals(6, violations().size(), String.join("\n", violations()));
    }

    @Test
    void printsTheEqualsLocation() {
        String equalsLocation = EXAMPLES.get(Money.class).getMethod("equals", Object.class)
                .getSourceCodeLocation().toString();

        assertTrue(violations().contains("Class <" + Money.class.getName()
                + "> overrides equals(Object) without hashCode() in " + equalsLocation),
                String.join("\n", violations()));
    }

    @Test
    void ignoresAClassDeclaringBoth() {
        assertNotReported("Sku");
    }

    @Test
    void ignoresHashCodeInheritedFromAProjectSuperclass() {
        assertNotReported("Customer");
    }

    @Test
    void ignoresHashCodeInheritedFromAJdkSuperclass() {
        assertNotReported("TagSet");
    }

    @Test
    void ignoresHashCodeWithoutEquals() {
        assertNotReported("Node");
        assertNotReported("BaseEntity");
    }

    @Test
    void ignoresAnEqualsOverload() {
        assertNotReported("Amount");
    }

    @Test
    void ignoresARecord() {
        assertNotReported("Coordinates");
    }

    @Test
    void ignoresAnInterfaceDeclaringEquals() {
        assertNotReported("EqualityContract");
    }

    @Test
    void ignoresAnAbstractEquals() {
        assertNotReported("AbstractValue");
    }

    @Test
    void ignoresAnAbstractHashCode() {
        assertNotReported("KeyedValue");
    }

    @Test
    void ignoresAnIdentityEqualsWithIdentityHashCode() {
        assertNotReported("IdentityRef");
    }

    @Test
    void ignoresASubclassInheritingEquals() {
        assertNotReported("FieldError");
    }

    @Test
    void ignoresObjectItself() {
        JavaClasses object = new ClassFileImporter().importClasses(Object.class);

        assertFalse(NoEqualsWithoutHashCodeRule.DEFINITION.evaluate(object).hasViolation(),
                NoEqualsWithoutHashCodeRule.DEFINITION.evaluate(object).getFailureReport().toString());
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoEqualsWithoutHashCodeRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("fixtures.Money>"), debt);
            assertTrue(debt.contains("fixtures.ProductCode>"), debt);
            assertTrue(debt.contains("fixtures.ValidationFailure>"), debt);
            assertTrue(debt.contains("fixtures.RouteList>"), debt);
            assertTrue(debt.contains("fixtures.IdentityToken>"), debt);
            assertTrue(debt.contains("fixtures.Barcode>"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
