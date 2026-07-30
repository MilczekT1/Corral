// Lives in the `groups` package so the pairing tests can read the package-private raw-rule and
// doc constants they pin.
package io.github.milczekt1.archrules.groups;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.archrules.RuleDoc;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;

/**
 * Shared machinery for the per-group tests that pin each public {@code @ArchTest} field to the raw
 * rule it is supposed to wrap.
 *
 * <p>Without this, {@code FrozenRules.freeze(NO_TX_ON_CLASSES_RULE, NO_TX_ON_METHODS_DOC)} — one
 * copy-paste slip — would pass every other test in the suite: the group tests exercise only the raw
 * constants, the id-pinning assertions look at {@code getDescription()} alone, and
 * {@code FreezingBehaviourTest} re-freezes the raw rule itself. The rule would be silently wrong in
 * every consumer forever.
 *
 * <p>The check seeds a throwaway freeze store from the public field and reads back what landed in
 * it. That pins two things at once: the store key really is the doc id (ArchUnit derives it from the
 * rule description), and the violations recorded under it really are the raw rule's.
 */
final class FrozenFieldStores {

    private static final String STORE_INDEX = "stored.rules";

    private FrozenFieldStores() {
    }

    /**
     * Points ArchUnit's default freeze store at {@code store} for the duration of one test.
     *
     * <p>{@link ArchConfiguration} is global process state and Surefire reuses one JVM, so every
     * caller must {@link #resetConfiguration()} afterwards — a leaked
     * {@code freeze.store.default.path} would corrupt unrelated test classes.
     */
    static void useTemporaryStore(Path store) {
        ArchConfiguration.get().setProperty("freeze.store.default.path", store.toString());
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreCreation", "true");
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreUpdate", "true");
    }

    static void resetConfiguration() {
        ArchConfiguration.get().reset();
    }

    /**
     * Asserts that {@code publicField} is a {@link FreezingArchRule} which freezes exactly
     * {@code rawRule} under {@code doc}'s id.
     */
    static void assertFreezes(ArchRule publicField, ArchRule rawRule, RuleDoc doc,
            JavaClasses fixtures, Path store) {
        Assertions.assertInstanceOf(FreezingArchRule.class, publicField,
                doc.id() + " must be wrapped by FrozenRules.freeze so adoption records debt "
                        + "instead of blocking");
        assertEquals(doc.id(), publicField.getDescription(),
                "the description IS the freeze-store key, so it must be the doc id");

        // A first run against a fresh store seeds it and must not fail; that is the whole point of
        // freezing. What it wrote is then compared with the raw rule's own verdict.
        publicField.check(fixtures);

        assertEquals(sorted(rawViolations(rawRule, fixtures)), sorted(storedViolations(store, doc.id())),
                "the public field published as " + doc.id() + " froze violations that are not the ones "
                        + "its raw *_RULE constant reports — check the FrozenRules.freeze(...) arguments");
    }

    /** What the raw constant reports on its own, i.e. the violations the field ought to freeze. */
    static List<String> rawViolations(ArchRule rawRule, JavaClasses fixtures) {
        return rawRule.allowEmptyShould(true).evaluate(fixtures).getFailureReport().getDetails();
    }

    /**
     * Reads the seeded store directly rather than trusting a second evaluation: the on-disk entry
     * keyed by the rule id is exactly what a consumer commits and what future runs diff against.
     */
    static List<String> storedViolations(Path store, String ruleId) {
        Properties index = new Properties();
        try (Reader reader = Files.newBufferedReader(store.resolve(STORE_INDEX), UTF_8)) {
            index.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("could not read the freeze store index at " + store, e);
        }

        String violationsFile = index.getProperty(ruleId);
        assertNotNull(violationsFile,
                "the freeze store has no entry keyed '" + ruleId + "'; keys present: "
                        + index.stringPropertyNames());
        try {
            return Files.readAllLines(store.resolve(violationsFile), UTF_8).stream()
                    .filter(line -> !line.isEmpty())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("could not read frozen violations for " + ruleId, e);
        }
    }

    private static List<String> sorted(List<String> lines) {
        return lines.stream().sorted().toList();
    }
}
