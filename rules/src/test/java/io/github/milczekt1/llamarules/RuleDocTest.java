package io.github.milczekt1.llamarules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
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
        assertEquals(Optional.of("do not do that"), doc.howNotToFix());
    }

    @Test
    void howNotToFixIsOptionalAndDefaultsToEmpty() {
        assertEquals(Optional.empty(), valid().build().howNotToFix());
    }

    @Test
    void rejectsBlankRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> valid().why("   ").build());
        assertThrows(IllegalArgumentException.class, () -> valid().howToFix(null).build());
        assertThrows(IllegalArgumentException.class, () -> valid().id("").build());
    }

    @Test
    void rejectsIdsThatWouldMakeUnstableOrUnreadableFreezeKeys() {
        // The id IS the freeze-store key: no spaces, no upper case, must be dot-namespaced.
        assertThrows(IllegalArgumentException.class, () -> valid().id("noSpringTransactional").build());
        assertThrows(IllegalArgumentException.class, () -> valid().id("db.No Spring Tx").build());
        assertThrows(IllegalArgumentException.class, () -> valid().id("DB.no-spring-tx").build());
    }

    @Test
    void blankFieldMessageNamesTheOffendingField() {
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> valid().why(" ").build());
        assertTrue(e.getMessage().contains("why"), "message should name the field: " + e.getMessage());
    }
}
