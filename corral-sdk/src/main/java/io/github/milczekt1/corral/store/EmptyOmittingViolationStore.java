package io.github.milczekt1.corral.store;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.TextFileBasedViolationStore;
import com.tngtech.archunit.library.freeze.ViolationStore;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * A {@link ViolationStore} that keeps the {@code stored.rules} index complete but writes no file for
 * a rule with zero violations, so a committed freeze store carries no empty files.
 *
 * <p>Register it in {@code archunit.properties}:
 * <pre>{@code freeze.store=io.github.milczekt1.corral.store.EmptyOmittingViolationStore}</pre>
 *
 * <p><strong>A clean rule stays frozen; its first later violation fails the build.</strong>
 * {@code FreezingArchRule} decides via {@link #contains}, which keys on the {@code stored.rules}
 * index entry, not on the file. "No file" means zero known violations, not unknown rule — only a
 * rule with no entry at all seeds and passes.
 *
 * <p><strong>Commit the {@code stored.rules} line</strong> that appears when a rule is first frozen.
 * Uncommitted, CI sees no entry, so the first violation is seeded as debt and the build stays green:
 * a rule that looks armed and is not.
 *
 * <p>Needs a public no-arg constructor — ArchUnit instantiates it reflectively.
 */
public class EmptyOmittingViolationStore implements ViolationStore {

    /**
     * The delegate's index file. Its {@code <rule-description>=<file-name>} layout is
     * {@link TextFileBasedViolationStore}'s undocumented internal format;
     * {@code storedRulesMapsRuleDescriptionToFileName} pins it so an ArchUnit upgrade that changes it
     * fails loudly rather than mishandling a consumer's store.
     */
    private static final String INDEX_FILE = "stored.rules";

    private static final String DEFAULT_STORE_PATH = "archunit_store";

    private final TextFileBasedViolationStore delegate =
            new TextFileBasedViolationStore(EmptyOmittingViolationStore::fileNameFor);

    private Path storePath;

    /**
     * Names a rule's file after its id, so {@code git log -p archunit/frozen/test.class-naming-convention}
     * reads as that rule's debt history instead of requiring a UUID lookup in the index.
     *
     * <p>Only ids qualify: {@code freeze.store} is global, so this store also serves rules frozen
     * without a {@code RuleDoc}, whose descriptions are whole sentences — spaces, quotes, no length
     * bound. Those keep ArchUnit's UUID. An id is lower-case, dot-and-hyphen only and short, so it is
     * safe as a file name on any filesystem.
     *
     * <p>Applied only to a rule with no index entry yet: {@code TextFileBasedViolationStore} reuses an
     * existing entry's file name. Stores written before this keep their UUIDs and keep working.
     */
    static String fileNameFor(String ruleDescription) {
        return RuleDoc.isId(ruleDescription) ? ruleDescription : UUID.randomUUID().toString();
    }

    @Override
    public void initialize(Properties properties) {
        delegate.initialize(properties);
        storePath = Path.of(properties.getProperty("default.path", DEFAULT_STORE_PATH));
    }

    @Override
    public boolean contains(ArchRule rule) {
        return delegate.contains(rule);
    }

    /**
     * Saves through the delegate first, always — that call is what writes the rule's index entry, and
     * an entry is what keeps the rule frozen. Only the resulting file is dropped when there is
     * nothing to record.
     */
    @Override
    public void save(ArchRule rule, List<String> violations) {
        delegate.save(rule, violations);
        if (violations.isEmpty()) {
            deleteViolationFileOf(rule);
        }
    }

    /** No file means zero known violations — the clean-rule case, not a missing store. */
    @Override
    public List<String> getViolations(ArchRule rule) {
        return hasViolationFile(rule) ? delegate.getViolations(rule) : List.of();
    }

    private boolean hasViolationFile(ArchRule rule) {
        return violationFileOf(rule).filter(Files::exists).isPresent();
    }

    private void deleteViolationFileOf(ArchRule rule) {
        Optional<Path> file = violationFileOf(rule);
        if (file.isEmpty()) {
            return;
        }
        try {
            Files.deleteIfExists(file.get());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Could not remove the empty violation file for rule '" + rule.getDescription() + "'", e);
        }
    }

    private Optional<Path> violationFileOf(ArchRule rule) {
        return Optional.ofNullable(readIndex().getProperty(rule.getDescription()))
                .map(storePath::resolve);
    }

    private Properties readIndex() {
        Properties index = new Properties();
        Path indexFile = storePath.resolve(INDEX_FILE);
        if (!Files.exists(indexFile)) {
            return index;
        }
        try (InputStream in = Files.newInputStream(indexFile)) {
            index.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + indexFile, e);
        }
        return index;
    }
}
