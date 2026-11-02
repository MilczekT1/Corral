package io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.CartLines;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.ConfigListener;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.Editor;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.HeaderIndex;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.ModernCart;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.ParserState;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.ReportRows;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.SettingsLoader;
import io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections.fixtures.UndoStack;
import io.github.milczekt1.corral.rules.testsupport.CommittedStore;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoLegacySynchronizedCollectionsRuleTest {

    private static final String ID = "corral.java.no-legacy-synchronized-collections";

    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            CartLines.class, ParserState.class, HeaderIndex.class, UndoStack.class, Editor.class,
            ModernCart.class, SettingsLoader.class, ConfigListener.class, ReportRows.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static final List<String> VIOLATIONS =
            NoLegacySynchronizedCollectionsRule.DEFINITION.evaluate(EXAMPLES).getFailureReport().getDetails();

    private static void assertFlagged(String subject, String target) {
        assertTrue(VIOLATIONS.stream().anyMatch(line -> line.contains(subject) && line.contains(target)),
                String.join("\n", VIOLATIONS));
    }

    private static void assertNotReported(String subject) {
        String report = String.join("\n", VIOLATIONS);

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
    void flagsAVectorParameter() {
        assertFlagged("ReportRows.load", "parameter of type <java.util.Vector>");
    }

    @Test
    void flagsAVectorTypeArgument() {
        assertFlagged("ReportRows.byRegion", "type argument depending on <java.util.Vector>");
    }

    @Test
    void flagsASubclassAtItsDeclaration() {
        assertFlagged("UndoStack", "extends class <java.util.Stack>");
    }

    @Test
    void reportsOnlyTheLegacyDependencies() {
        assertEquals(18, VIOLATIONS.size(), String.join("\n", VIOLATIONS));
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

    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        String debt = CommittedStore.freeze(NoLegacySynchronizedCollectionsRule.rule, ID, EXAMPLES);

        assertTrue(debt.contains("CartLines.items"), debt);
        assertTrue(debt.contains("ParserState.depth"), debt);
        assertTrue(debt.contains("UndoStack"), debt);
    }
}
