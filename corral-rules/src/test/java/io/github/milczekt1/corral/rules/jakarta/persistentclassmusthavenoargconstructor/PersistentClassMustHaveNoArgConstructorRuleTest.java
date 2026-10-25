package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.AbstractAccount;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.BaseEntity;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.Coupon;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.Dimensions;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.DiscountRate;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.GeoPoint;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.GiftCard;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.Invoice;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.LegacyAddress;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.LegacyCustomer;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.LegacyPhoneNumber;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.LegacyPostcode;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.Money;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.OrderSummary;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.Payment;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.Product;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.Shipment;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.Supplier;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.Temperature;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.Ticket;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.Warehouse;
import io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures.Weight;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import jakarta.persistence.Entity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodCall;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PersistentClassMustHaveNoArgConstructorRuleTest {

    private static final String ID = "corral.jakarta.persistent-class-must-have-no-arg-constructor";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importPackagesOf(Invoice.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations(JavaClasses classes) {
        return PersistentClassMustHaveNoArgConstructorRule.DEFINITION.evaluate(classes)
                .getFailureReport().getDetails();
    }

    private static void assertFlagged(Class<?> type) {
        assertFlagged(type.getName());
    }

    private static void assertFlagged(String className) {
        List<String> violations = violations(EXAMPLES);

        assertTrue(violations.stream().anyMatch(line -> line.contains("<" + className + ">")),
                String.join("\n", violations));
    }

    private static void assertNotReported(Class<?> type) {
        String report = String.join("\n", violations(EXAMPLES));

        assertFalse(report.contains("<" + type.getName() + ">"), report);
    }

    @Test
    void flagsAnEntityWithOnlyAnAllArgsConstructor() {
        assertFlagged(Invoice.class);
    }

    @Test
    void flagsAnEntityWhoseOnlyNoArgConstructorIsPrivate() {
        assertFlagged(Shipment.class);
    }

    @Test
    void flagsARecordEntity() {
        assertFlagged(Coupon.class);
    }

    @Test
    void flagsAJavaxEntity() {
        assertFlagged(LegacyCustomer.class);
    }

    @Test
    void flagsAJakartaEmbeddableWithOnlyAnAllArgsConstructor() {
        assertFlagged(Money.class);
    }

    @Test
    void flagsAJavaxRecordEmbeddable() {
        assertFlagged(LegacyAddress.class);
    }

    @Test
    void flagsANonStaticInnerEntity() {
        assertFlagged(Warehouse.Bay.class);
    }

    @Test
    void flagsAnAbstractEntity() {
        assertFlagged(AbstractAccount.class);
    }

    @Test
    void flagsAnEntityWhoseConstructorIsAnnotatedInstantiator() {
        assertFlagged(Ticket.class);
    }

    @Test
    void flagsAJavaxEmbeddableWhoseConstructorIsAnnotatedInstantiator() {
        assertFlagged(LegacyPostcode.class);
    }

    @Test
    void flagsAJakartaEmbeddableWhoseInstantiatorArrivesThroughAComposedAnnotation() {
        assertFlagged(DiscountRate.class);
    }

    @Test
    void flagsAnEntityAnnotatedEmbeddableInstantiator() {
        assertFlagged(GiftCard.class);
    }

    @Test
    void flagsAJavaxEmbeddableAnnotatedEmbeddableInstantiator() {
        assertFlagged(LegacyPhoneNumber.class);
    }

    @Test
    void flagsAJakartaEmbeddableWhoseInstantiatorConstructorIsNotPublic() {
        assertFlagged(Weight.class);
    }

    @Test
    void flagsAJakartaEmbeddableThatIsNotPublicDespiteAPublicInstantiatorConstructor() {
        assertFlagged(Invoice.class.getPackageName() + ".Distance");
    }

    @Test
    void reportsOneViolationPerFlaggedClass() {
        List<String> violations = violations(EXAMPLES);

        assertEquals(15, violations.size(), String.join("\n", violations));
        long distinctClasses = violations.stream()
                .map(line -> line.substring(line.indexOf('<'), line.indexOf('>') + 1))
                .distinct()
                .count();
        assertEquals(violations.size(), distinctClasses, String.join("\n", violations));
    }

    @Test
    void reportsAMissingAndAPrivateConstructorInOneShape() {
        List<String> violations = violations(EXAMPLES);

        assertTrue(violations.contains("Class <" + Invoice.class.getName()
                + "> does not declare a non-private no-arg constructor in (Invoice.java:0)"),
                String.join("\n", violations));
        assertTrue(violations.contains("Class <" + Shipment.class.getName()
                + "> does not declare a non-private no-arg constructor in (Shipment.java:0)"),
                String.join("\n", violations));
    }

    @Test
    void ignoresAProtectedNoArgConstructor() {
        assertNotReported(Payment.class);
    }

    @Test
    void ignoresTheImplicitDefaultConstructor() {
        assertNotReported(Product.class);
    }

    @Test
    void ignoresAPackagePrivateNoArgConstructor() {
        assertNotReported(Supplier.class);
    }

    @Test
    void ignoresAJakartaRecordEmbeddable() {
        assertNotReported(GeoPoint.class);
    }

    @Test
    void ignoresAJakartaEmbeddableWithAnInstantiatorConstructor() {
        assertNotReported(Dimensions.class);
    }

    @Test
    void ignoresAJakartaEmbeddableAnnotatedEmbeddableInstantiator() {
        assertNotReported(Temperature.class);
    }

    @Test
    void ignoresAMappedSuperclass() {
        assertNotReported(BaseEntity.class);
    }

    @Test
    void ignoresAClassWithNoPersistenceAnnotation() {
        assertNotReported(OrderSummary.class);
    }

    /** What the Kotlin no-arg plugin emits: a public synthetic no-arg constructor beside the real one. */
    @Test
    void ignoresASyntheticNoArgConstructor(@TempDir Path classes) throws Exception {
        String name = Invoice.class.getPackageName() + ".KotlinCustomer";
        new ByteBuddy()
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(name)
                .annotateType(AnnotationDescription.Builder.ofType(Entity.class).build())
                .defineConstructor(Visibility.PUBLIC, SyntheticState.SYNTHETIC)
                .intercept(MethodCall.invoke(Object.class.getConstructor()))
                .defineConstructor(Visibility.PUBLIC).withParameters(Long.class)
                .intercept(MethodCall.invoke(Object.class.getConstructor()))
                .make()
                .saveIn(classes.toFile());
        JavaClasses generated = new ClassFileImporter().importPath(classes);

        JavaClass kotlinCustomer = generated.get(name);
        assertTrue(kotlinCustomer.getConstructors().stream()
                        .anyMatch(constructor -> constructor.getModifiers().contains(JavaModifier.SYNTHETIC)),
                "the generated no-arg constructor must be imported as synthetic, or this test proves nothing");
        assertEquals(List.of(), violations(generated));
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class,
                    PersistentClassMustHaveNoArgConstructorRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("<" + Invoice.class.getName() + ">"), debt);
            assertTrue(debt.contains("<" + Money.class.getName() + ">"), debt);
            assertTrue(debt.contains("<" + LegacyAddress.class.getName() + ">"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
