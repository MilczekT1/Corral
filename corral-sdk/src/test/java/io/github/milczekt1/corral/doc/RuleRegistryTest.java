package io.github.milczekt1.corral.doc;

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
        RuleDoc registered = doc("test.no-registry-find-me-fixture", "because");
        RuleRegistry.register(registered);

        assertEquals(Optional.of(registered), RuleRegistry.find("test.no-registry-find-me-fixture"));
    }

    @Test
    void findReturnsEmptyForUnknownId() {
        assertEquals(Optional.empty(), RuleRegistry.find("test.no-registry-never-registered-fixture"));
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
        RuleRegistry.register(doc("test.no-registry-idempotent-fixture", "because"));
        RuleRegistry.register(doc("test.no-registry-idempotent-fixture", "because"));

        assertEquals(Optional.of(doc("test.no-registry-idempotent-fixture", "because")),
                RuleRegistry.find("test.no-registry-idempotent-fixture"));
    }

    @Test
    void rejectsTwoDifferentDocsSharingAnId() {
        RuleRegistry.register(doc("test.no-registry-clash-fixture", "first reason"));
        RuleDoc sameIdDifferentDoc = doc("test.no-registry-clash-fixture", "conflicting reason");

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> RuleRegistry.register(sameIdDifferentDoc));
        assertTrue(e.getMessage().contains("test.no-registry-clash-fixture"), e.getMessage());
    }

    @Test
    void allIsSortedByIdAndContainsRegisteredDocs() {
        RuleRegistry.register(doc("test.no-registry-zzz-last-fixture", "because"));
        RuleRegistry.register(doc("test.no-registry-aaa-first-fixture", "because"));

        List<String> ids = RuleRegistry.all().stream().map(RuleDoc::id).toList();

        assertEquals(ids.stream().sorted().toList(), ids, "all() must be sorted by id");
        assertTrue(ids.contains("test.no-registry-aaa-first-fixture"));
        assertTrue(ids.contains("test.no-registry-zzz-last-fixture"));
    }
}
