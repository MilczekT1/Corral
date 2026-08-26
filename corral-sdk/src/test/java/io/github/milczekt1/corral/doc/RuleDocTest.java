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
        // The id pattern repeats a group possessively; two segments and three must behave alike.
        assertEquals("logging.no-system-out", valid().id("logging.no-system-out").build().id());
        assertEquals("test.mockito.no-static-mocking", valid().id("test.mockito.no-static-mocking").build().id());
        assertTrue(RuleDoc.isId("a.b.c.d.e.f.g.h.i.j"));
    }

    /**
     * {@code isId} alone is intentionally weaker than the constructor: it backs the constructor's
     * shape-only error path, so a too-long or too-deep string still counts as "id-shaped". A caller
     * that also needs the caps enforced — {@code EmptyOmittingViolationStore}'s filename gate — uses
     * {@code isIdWithinCaps} instead.
     */
    @Test
    void isIdWithinCapsAlsoEnforcesTheLengthAndSegmentCaps() {
        assertTrue(RuleDoc.isIdWithinCaps("logging.no-system-out"));
        assertTrue(RuleDoc.isId("a.b.c.d.e.f.g.h.i.j"), "isId itself stays shape-only");
        assertFalse(RuleDoc.isIdWithinCaps("a.b.c.d.e.f.g.h.i.j"), "but isIdWithinCaps caps depth");
        assertFalse(RuleDoc.isIdWithinCaps("security." + "a".repeat(70)), "and caps length");
        assertFalse(RuleDoc.isIdWithinCaps(null));
        assertFalse(RuleDoc.isIdWithinCaps("not an id"));
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

    /**
     * {@code RuleDoc} is public SDK surface: a consumer's {@code DocumentedRule} builds one with
     * their own namespace, not Corral's. What namespace is allowed, and whether a slug carries a
     * polarity marker, is catalog taxonomy for Corral's own rules — enforced by {@code corral-rules}'
     * {@code RuleIdGrammarTest} against {@code AllCentralRules} only, never here.
     */
    @Test
    void acceptsAnyNamespaceAndSlugAConsumerChooses() {
        assertDoesNotThrow(() -> valid().id("acme.no-stdout-in-services").build(),
                "a consumer's own namespace must keep working — that is the whole point of the SDK");
        assertDoesNotThrow(() -> valid().id("db.no-spring-transactional").build());
        assertDoesNotThrow(() -> valid().id("naming.lowercase-packages").build(),
                "RuleDoc itself has no opinion on polarity markers");
        assertDoesNotThrow(() -> valid().id("corral.exclusions-resolve").build());
    }

    @Test
    void acceptsAnIdWithExactlyThreeSegments() {
        assertDoesNotThrow(() -> valid().id("spring.data.no-repository-in-controller").build(),
                "a third segment is a hygiene, not a taxonomy, question at this layer");
    }

    @Test
    void rejectsAnIdWithMoreThanFourSegments() {
        // The id becomes a file name in every consumer's freeze store, so depth is a hygiene cap
        // that binds every rule author, not catalog taxonomy for what a segment may contain.
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> valid().id("spring.data.jpa.mockito.no-repository-in-controller").build());

        assertTrue(thrown.getMessage().contains("4"), thrown.getMessage());
    }

    @Test
    void rejectsAnIdLongerThanSeventyTwoCharacters() {
        String tooLong = "security." + "a".repeat(70);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> valid().id(tooLong).build());

        assertTrue(thrown.getMessage().contains("72"), thrown.getMessage());
    }

}
