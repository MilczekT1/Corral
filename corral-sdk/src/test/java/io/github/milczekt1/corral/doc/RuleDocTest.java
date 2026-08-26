package io.github.milczekt1.corral.doc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuleDocTest {

    private static RuleDoc.Builder valid() {
        return RuleDoc.builder()
                .id("logging.no-system-out")
                .why("because reasons")
                .howToFix("do this instead");
    }

    @Test
    void buildsWithAllFields() {
        RuleDoc doc = valid().howNotToFix("do not do that").build();

        assertEquals("logging.no-system-out", doc.id());
        assertEquals("because reasons", doc.why());
        assertEquals("do this instead", doc.howToFix());
        assertEquals("do not do that", doc.howNotToFix());
        assertTrue(doc.hasHowNotToFix());
    }

    @Test
    void howNotToFixIsOptionalAndDefaultsToAbsent() {
        assertFalse(valid().build().hasHowNotToFix());
    }

    @Test
    void rejectsBlankRequiredFields() {
        RuleDoc.Builder whitespaceOnlyWhy = valid().why("   ");
        RuleDoc.Builder nullHowToFix = valid().howToFix(null);
        RuleDoc.Builder emptyId = valid().id("");

        assertThrows(IllegalArgumentException.class, whitespaceOnlyWhy::build);
        assertThrows(IllegalArgumentException.class, nullHowToFix::build);
        assertThrows(IllegalArgumentException.class, emptyId::build);
    }

    @Test
    void acceptsIdsOfAnyNamespaceDepth() {
        // The id pattern repeats a group possessively; two segments and ten must behave alike.
        assertEquals("logging.no-system-out", valid().id("logging.no-system-out").build().id());
        assertEquals("test.mockito.no-static-mocking", valid().id("test.mockito.no-static-mocking").build().id());
        assertTrue(RuleDoc.isId("a.b.c.d.e.f.g.h.i.j"));
    }

    @Test
    void rejectsIdsThatWouldMakeUnstableOrUnreadableFreezeKeys() {
        // The id IS the freeze-store key: no spaces, no upper case, must be dot-namespaced.
        RuleDoc.Builder notNamespaced = valid().id("noSpringTransactional");
        RuleDoc.Builder containsSpaces = valid().id("db.No Spring Tx");
        RuleDoc.Builder upperCaseNamespace = valid().id("DB.no-spring-tx");

        assertThrows(IllegalArgumentException.class, notNamespaced::build);
        assertThrows(IllegalArgumentException.class, containsSpaces::build);
        assertThrows(IllegalArgumentException.class, upperCaseNamespace::build);
    }

    @Test
    void acceptsEveryShapeTheGrammarAllows() {
        assertDoesNotThrow(() -> valid().id("api.no-array-return-types").build());
        assertDoesNotThrow(() -> valid().id("test.mockito.no-static-mocking").build());
        assertDoesNotThrow(() -> valid().id("exception.fields-must-be-final").build());
        assertDoesNotThrow(() -> valid().id("java21.no-blocking-in-virtual-thread").build());
        assertDoesNotThrow(() -> valid().id("corral.exclusions-resolve").build(),
                "corral.* holds Corral's own meta-checks and is exempt from the polarity marker");
    }

    @Test
    void rejectsASegmentOutsideTheClosedVocabulary() {
        // The vocabulary is closed so that "which bucket" never becomes an editorial argument, which
        // is what produces renames — and a rename is a silent loss of enforcement.
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> valid().id("persistence.no-final-entity").build());

        assertTrue(thrown.getMessage().contains("persistence"), thrown.getMessage());
    }

    @Test
    void rejectsASlugWithNoPolarityMarker() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> valid().id("naming.lowercase-packages").build());

        assertTrue(thrown.getMessage().contains("no-"), thrown.getMessage());
    }

    @Test
    void rejectsAThirdSegmentThatIsNotALibraryOrJavaVersion() {
        // A sub-concern in the key is mutable taxonomy in an immutable slot. Groups exist for that.
        assertThrows(IllegalArgumentException.class,
                () -> valid().id("spring.data.no-repository-in-controller").build());
    }

    @Test
    void rejectsAnIdLongerThanSixtyCharacters() {
        String tooLong = "security." + "a".repeat(60);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> valid().id(tooLong).build());

        assertTrue(thrown.getMessage().contains("60"), thrown.getMessage());
    }

}
