package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.AbstractLineItem;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.AuditEntry;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.CancelOrder;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.CartDto;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.Command;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.CurrencyCode;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.Customer;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.ExpressPayment;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.InvoiceDto;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.Money;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.OrderDto;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.PaymentDeclinedException;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.PaymentDto;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.RefundDto;
import io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures.ShipmentDto;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SerializableMustDeclareSerialVersionUidRuleTest {

    private static final String ID = "corral.java.serializable-must-declare-serial-version-uid";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            OrderDto.class, ShipmentDto.class, CartDto.class, RefundDto.class, AuditEntry.class,
            AbstractLineItem.class, CancelOrder.class, ExpressPayment.class, PaymentDto.class, InvoiceDto.class,
            Customer.class, CurrencyCode.class, Money.class, Command.class, PaymentDeclinedException.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return SerializableMustDeclareSerialVersionUidRule.DEFINITION.evaluate(EXAMPLES)
                .getFailureReport().getDetails();
    }

    private static void assertFlagged(String subject, String reason) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains(subject) && line.contains(reason)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violations());

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsAMissingUidBesideAStaticFinalLongOfAnotherName() {
        assertFlagged("OrderDto", "declares no serialVersionUID");
    }

    @Test
    void flagsANonFinalUid() {
        assertFlagged("ShipmentDto.serialVersionUID", "is not static final long");
    }

    @Test
    void flagsANonStaticUid() {
        assertFlagged("CartDto.serialVersionUID", "is not static final long");
    }

    @Test
    void flagsAnIntUid() {
        assertFlagged("RefundDto.serialVersionUID", "is not static final long");
    }

    @Test
    void flagsAnExternalizableClass() {
        assertFlagged("AuditEntry", "declares no serialVersionUID");
    }

    @Test
    void flagsAnAbstractClass() {
        assertFlagged("AbstractLineItem", "declares no serialVersionUID");
    }

    @Test
    void flagsAClassSerializableThroughAProjectInterface() {
        assertFlagged("CancelOrder", "declares no serialVersionUID");
    }

    @Test
    void flagsASubclassWhoseSuperclassDeclaresTheUid() {
        assertFlagged("ExpressPayment", "declares no serialVersionUID");
    }

    @Test
    void reportsOneViolationPerFlaggedClass() {
        List<String> violations = violations();

        assertEquals(8, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresAPrivateStaticFinalLongUid() {
        assertNotReported("PaymentDto");
    }

    @Test
    void ignoresAPublicStaticFinalLongUid() {
        assertNotReported("InvoiceDto");
    }

    @Test
    void ignoresAClassThatIsNotSerializable() {
        assertNotReported("Customer");
    }

    @Test
    void ignoresAnEnum() {
        assertNotReported("CurrencyCode");
    }

    @Test
    void ignoresARecord() {
        assertNotReported("Money");
    }

    @Test
    void ignoresAnInterface() {
        assertNotReported("Command");
    }

    @Test
    void ignoresAnExceptionSerializableOnlyThroughThrowable() {
        assertNotReported("PaymentDeclinedException");
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
                    SerializableMustDeclareSerialVersionUidRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("OrderDto"), debt);
            assertTrue(debt.contains("ShipmentDto.serialVersionUID"), debt);
            assertTrue(debt.contains("AuditEntry"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
