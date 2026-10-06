package io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.CartLines;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.ConfigListener;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.Editor;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.HeaderIndex;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.ModernCart;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.ParserState;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.SettingsLoader;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.UndoStack;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoLegacySynchronizedCollectionsRuleTest {

    private static final String ID = "corral.java.no-legacy-synchronized-collections";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            CartLines.class, ParserState.class, HeaderIndex.class, UndoStack.class, Editor.class,
            ModernCart.class, SettingsLoader.class, ConfigListener.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static List<String> violations() {
        return NoLegacySynchronizedCollectionsRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();
    }

    private static void assertFlagged(String subject, String target) {
        List<String> violations = violations();

        assertTrue(violations.stream().anyMatch(line -> line.contains(subject) && line.contains(target)),
                String.join("\n", violations));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", violations());

        assertFalse(report.contains(subject), report);
    }

    @Test
    void flagsAVectorField() {
        assertFlagged("CartLines.items", "<java.util.Vector>");
    }

    @Test
    void flagsAHashtableField() {
        assertFlagged("CartLines.labels", "<java.util.Hashtable>");
    }

    @Test
    void flagsAVectorConstructedBehindAListField() {
        assertFlagged("ParserState.<init>", "java.util.Vector.<init>");
    }

    @Test
    void flagsAStackConstructedInAMethod() {
        assertFlagged("ParserState.depth", "java.util.Stack.<init>");
    }

    @Test
    void flagsAHashtableReturnType() {
        assertFlagged("HeaderIndex.headers", "return type <java.util.Hashtable>");
    }

    @Test
    void flagsASubclassAtItsDeclaration() {
        assertFlagged("UndoStack", "extends class <java.util.Stack>");
    }

    @Test
    void reportsOnlyTheLegacyDependencies() {
        List<String> violations = violations();

        assertEquals(15, violations.size(), String.join("\n", violations));
    }

    @Test
    void ignoresTheArrayListBesideAFlaggedField() {
        assertNotReported("CartLines.tags");
    }

    @Test
    void ignoresTheArrayDequeBesideAFlaggedStack() {
        assertNotReported("ArrayDeque");
    }

    @Test
    void ignoresAUseOfTheSubclass() {
        assertNotReported("Editor");
    }

    @Test
    void ignoresTheModernReplacements() {
        assertNotReported("ModernCart");
    }

    @Test
    void ignoresProperties() {
        assertNotReported("SettingsLoader");
    }

    @Test
    void ignoresDictionary() {
        assertNotReported("ConfigListener");
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
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoLegacySynchronizedCollectionsRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("CartLines.items"), debt);
            assertTrue(debt.contains("ParserState.depth"), debt);
            assertTrue(debt.contains("UndoStack"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
