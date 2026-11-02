package io.github.milczekt1.corral.rules.java.noequalswithouthashcode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
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
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import io.github.milczekt1.corral.rules.testsupport.FixtureVerdicts;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoEqualsWithoutHashCodeRuleTest {

    private static final String ID = "corral.java.no-equals-without-hashcode";

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

    @Test
    void everyFixtureMatchesItsVerdict() {
        FixtureVerdicts.assertMatch(NoEqualsWithoutHashCodeRuleTest.class, NoEqualsWithoutHashCodeRule.DEFINITION, EXAMPLES);
    }

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoEqualsWithoutHashCodeRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("fixtures.Money>"), debt);
        assertTrue(debt.contains("fixtures.ProductCode>"), debt);
        assertTrue(debt.contains("fixtures.ValidationFailure>"), debt);
        assertTrue(debt.contains("fixtures.RouteList>"), debt);
        assertTrue(debt.contains("fixtures.IdentityToken>"), debt);
        assertTrue(debt.contains("fixtures.Barcode>"), debt);
    }
}
