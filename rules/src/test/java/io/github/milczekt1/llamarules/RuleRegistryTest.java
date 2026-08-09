package io.github.milczekt1.llamarules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RuleRegistryTest {

    private static RuleDoc doc(String id, String why) {
        return RuleDoc.builder().id(id).why(why).howToFix("fix it").build();
    }

    @Test
    void registersAndFindsById() {
        RuleDoc registered = doc("registry.find-me", "because");
        RuleRegistry.register(registered);

        assertEquals(Optional.of(registered), RuleRegistry.find("registry.find-me"));
    }

    @Test
    void findReturnsEmptyForUnknownId() {
        assertEquals(Optional.empty(), RuleRegistry.find("registry.never-registered"));
    }

    @Test
    void findToleratesNullAndArbitraryDescriptions() {
        // The formatter passes ArchUnit rule descriptions straight through, including
        // full sentences from a consumer's own rules. This must never blow up.
        assertEquals(Optional.empty(), RuleRegistry.find(null));
        assertEquals(Optional.empty(), RuleRegistry.find("no classes should be annotated with @Foo"));
    }

    @Test
    void reRegisteringTheSameDocIsIdempotent() {
        RuleRegistry.register(doc("registry.idempotent", "because"));
        RuleRegistry.register(doc("registry.idempotent", "because"));

        assertEquals(Optional.of(doc("registry.idempotent", "because")),
                RuleRegistry.find("registry.idempotent"));
    }

    @Test
    void rejectsTwoDifferentDocsSharingAnId() {
        RuleRegistry.register(doc("registry.clash", "first reason"));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> RuleRegistry.register(doc("registry.clash", "conflicting reason")));
        assertTrue(e.getMessage().contains("registry.clash"), e.getMessage());
    }

    @Test
    void allIsSortedByIdAndContainsRegisteredDocs() {
        RuleRegistry.register(doc("registry.zzz-last", "because"));
        RuleRegistry.register(doc("registry.aaa-first", "because"));

        List<String> ids = RuleRegistry.all().stream().map(RuleDoc::id).toList();

        assertEquals(ids.stream().sorted().toList(), ids, "all() must be sorted by id");
        assertTrue(ids.contains("registry.aaa-first"));
        assertTrue(ids.contains("registry.zzz-last"));
    }
}
