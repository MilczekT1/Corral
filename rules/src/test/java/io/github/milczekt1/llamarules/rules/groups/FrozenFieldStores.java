package io.github.milczekt1.llamarules.rules.groups;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.llamarules.rules.RuleDoc;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;

/**
 * Pins each public {@code @ArchTest} field to the raw rule it wraps.
 *
 * <p>Catches a copy-paste slip like {@code freeze(RULE_A, DOC_B)}, which every other test would miss:
 * rule tests exercise only the raw constants, and id assertions look at {@code getDescription()}
 * alone. The rule would be wrong in every consumer, silently.
 *
 * <p>Seeds a throwaway store from the public field and reads it back, pinning both that the store
 * key is the doc id and that the recorded violations are the raw rule's.
 *
 * <p>Public because callers live one package per rule, outside {@code groups}.
 */
public final class FrozenFieldStores {

    private static final String STORE_INDEX = "stored.rules";

    private FrozenFieldStores() {
    }

    /**
     * Points ArchUnit's default freeze store at {@code store} for one test.
     *
     * <p>{@link ArchConfiguration} is global and Surefire reuses one JVM, so callers must
     * {@link #resetConfiguration()} afterwards or they corrupt unrelated test classes.
     */
    public static void useTemporaryStore(Path store) {
        ArchConfiguration.get().setProperty("freeze.store.default.path", store.toString());
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreCreation", "true");
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreUpdate", "true");
    }

    public static void resetConfiguration() {
        ArchConfiguration.get().reset();
    }

    /**
     * Asserts that {@code publicField} is a {@link FreezingArchRule} which freezes exactly
     * {@code rawRule} under {@code doc}'s id.
     */
    public static void assertFreezes(ArchRule publicField, ArchRule rawRule, RuleDoc doc,
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
