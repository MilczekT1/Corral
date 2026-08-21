package io.github.milczekt1.llamaguard.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuleDocTest {

    private static RuleDoc.Builder valid() {
        return RuleDoc.builder()
                .id("db.no-spring-transactional")
                .why("because reasons")
                .howToFix("do this instead");
    }

    @Test
    void buildsWithAllFields() {
        RuleDoc doc = valid().howNotToFix("do not do that").build();

        assertEquals("db.no-spring-transactional", doc.id());
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
        assertEquals("db.no-spring-transactional", valid().id("db.no-spring-transactional").build().id());
        assertEquals("db.jpa.no-open-session-in-view", valid().id("db.jpa.no-open-session-in-view").build().id());
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

}
