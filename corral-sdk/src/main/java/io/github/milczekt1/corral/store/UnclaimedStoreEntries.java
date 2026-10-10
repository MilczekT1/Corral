package io.github.milczekt1.corral.store;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

import com.tngtech.archunit.ArchConfiguration;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;

/**
 * Finds {@code stored.rules} entries whose id no registered rule claims. ArchUnit never prunes the
 * store, so an entry outlives the rule that wrote it.
 *
 * <p>Reads the store at {@code freeze.store.default.path} directly, so it works with ArchUnit's stock
 * store as well as {@link EmptyOmittingViolationStore}. Keys that are not id-shaped belong to rules
 * frozen without a {@code RuleDoc} and are never reported.
 */
@UtilityClass
public class UnclaimedStoreEntries {

    static final String STORE_PATH_PROPERTY = "freeze.store.default.path";

    static final int LISTED_AT_MOST = 5;

    /**
     * The unclaimed entries, or {@code null} when there are none or there is no {@code stored.rules}.
     * Live at call time: rules register while their classes initialise.
     */
    public static String warning() {
        String storePath = ArchConfiguration.get()
                .getPropertyOrDefault(STORE_PATH_PROPERTY, EmptyOmittingViolationStore.DEFAULT_STORE_PATH);
        return warning(Path.of(storePath), id -> RuleRegistry.find(id).isPresent());
    }

    static String warning(Path storePath, Predicate<String> isRegistered) {
        Path index = storePath.resolve(EmptyOmittingViolationStore.INDEX_FILE);
        if (!Files.isRegularFile(index)) {
            return null;
        }

        List<String> unclaimed;
        try {
            unclaimed = keysOf(index).stream()
                    .filter(RuleDoc::isId)
                    .filter(not(isRegistered))
                    .sorted()
                    .toList();
        } catch (IOException | IllegalArgumentException e) {
            return "Could not read " + index + " to look for entries no rule claims: " + e;
        }

        if (unclaimed.isEmpty()) {
            return null;
        }
        return """
                %s: %s no rule registered in this run:
                %s
                If you ran a single rule this is expected. In a full run each was left behind by a rule \
                you deleted or stopped wiring. ArchUnit never prunes the store: delete the entry's line \
                and the file that line names.\
                """.formatted(index, unclaimed.size() == 1 ? "1 entry names" : unclaimed.size() + " entries name",
                        listed(unclaimed));
    }

    private static String listed(List<String> ids) {
        String shown = ids.stream()
                .limit(LISTED_AT_MOST)
                .map(id -> "  " + id)
                .collect(joining(System.lineSeparator()));
        int hidden = ids.size() - LISTED_AT_MOST;
        return hidden > 0 ? shown + System.lineSeparator() + "  ... and " + hidden + " more" : shown;
    }

    /** {@link Properties}, not a line split: a key can be a whole sentence with escaped spaces. */
    private static List<String> keysOf(Path index) throws IOException {
        Properties entries = new Properties();
        try (InputStream in = Files.newInputStream(index)) {
            entries.load(in);
        }
        return entries.stringPropertyNames().stream().toList();
    }
}
