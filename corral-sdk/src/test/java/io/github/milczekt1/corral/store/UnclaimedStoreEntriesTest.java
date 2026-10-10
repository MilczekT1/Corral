package io.github.milczekt1.corral.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UnclaimedStoreEntriesTest {

    @TempDir
    Path storeDir;

    private void writeIndex(String... keys) throws IOException {
        Properties index = new Properties();
        for (String key : keys) {
            index.setProperty(key, key);
        }
        try (OutputStream out = Files.newOutputStream(storeDir.resolve("stored.rules"))) {
            index.store(out, null);
        }
    }

    private String warningClaiming(String... registeredIds) {
        return UnclaimedStoreEntries.warning(storeDir, Set.of(registeredIds)::contains);
    }

    @Test
    void noIndexWarnsNothing() {
        assertNull(warningClaiming());
    }

    @Test
    void anIndexWhoseIdsAreAllClaimedWarnsNothing() throws IOException {
        writeIndex("test.no-claimed-a", "test.no-claimed-b");

        assertNull(warningClaiming("test.no-claimed-a", "test.no-claimed-b"));
    }

    @Test
    void anUnclaimedIdIsNamedAndAClaimedOneIsNot() throws IOException {
        writeIndex("test.no-left-behind", "test.no-still-registered");

        String message = warningClaiming("test.no-still-registered");

        assertTrue(message.contains("test.no-left-behind"), message);
        assertFalse(message.contains("test.no-still-registered"), message);
        assertTrue(message.contains(storeDir.resolve("stored.rules").toString()), message);
        assertTrue(message.contains("1 entry names no rule"), message);
    }

    /** Rules frozen without a {@code RuleDoc} are keyed by their sentence description. */
    @Test
    void aKeyThatIsNotAnIdIsNeverReported() throws IOException {
        writeIndex("classes should have simple name 'Whatever'");

        assertNull(warningClaiming());
    }

    @Test
    void theListIsCappedAndCountsTheRest() throws IOException {
        writeIndex("test.no-g", "test.no-a", "test.no-f", "test.no-b", "test.no-e", "test.no-d", "test.no-c");

        String message = warningClaiming();

        assertTrue(message.contains("7 entries name no rule"), message);
        List<String> listed = message.lines().filter(line -> line.startsWith("  test.no-")).toList();
        assertEquals(List.of("  test.no-a", "  test.no-b", "  test.no-c", "  test.no-d", "  test.no-e"), listed,
                "the first " + UnclaimedStoreEntries.LISTED_AT_MOST + " in sorted order");
        assertTrue(message.contains("  ... and 2 more"), message);
    }

    @Test
    void anUnreadableIndexIsReportedNotThrown() throws IOException {
        Files.writeString(storeDir.resolve("stored.rules"), "test.no-broken=\\uZZZZ");

        String message = warningClaiming();

        assertTrue(message.startsWith("Could not read "), message);
    }

    /** The production entry point: store path from {@code ArchConfiguration}, claims from {@link RuleRegistry}. */
    @Test
    void theProductionWarningReadsTheConfiguredStoreAndTheRegistry() throws IOException {
        RuleRegistry.register(RuleDoc.builder()
                .id("test.no-unclaimed-production-registered").why("because").howToFix("fix it").build());
        writeIndex("test.no-unclaimed-production-registered", "test.no-unclaimed-production-orphan");

        String message;
        try {
            ArchConfiguration.get().setProperty(UnclaimedStoreEntries.STORE_PATH_PROPERTY, storeDir.toString());
            message = UnclaimedStoreEntries.warning();
        } finally {
            ArchConfiguration.get().reset();
        }

        assertTrue(message.contains("test.no-unclaimed-production-orphan"), message);
        assertFalse(message.contains("test.no-unclaimed-production-registered"), message);
    }
}
