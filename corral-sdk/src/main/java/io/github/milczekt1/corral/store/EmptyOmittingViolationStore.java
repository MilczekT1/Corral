package io.github.milczekt1.corral.store;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.TextFileBasedViolationStore;
import com.tngtech.archunit.library.freeze.ViolationStore;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        rewriteIndexSorted();
    }

    /**
     * Rewrites {@code stored.rules} with its lines in key order.
     *
     * <p>{@link java.util.Properties#store} sorts by key only from JDK 21. On 17-20 it writes the
     * iteration order of its internal map, which reshuffles wholesale whenever that map resizes — so a
     * consumer's committed store would produce an unreviewable diff on upgrade, on some JDKs and not
     * others. Writing the file here makes the order a property of this store instead of the JRE.
     *
     * <p>Runs after the delegate's own write, which is what creates the entry being reordered.
     */
    private void rewriteIndexSorted() {
        Path indexFile = storePath.resolve(INDEX_FILE);
        if (!Files.exists(indexFile)) {
            return;
        }
        try {
            Files.write(indexFile, sortedLines(readIndex()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not rewrite " + indexFile + " in sorted order", e);
        }
    }

    /**
     * Renders {@code index} as {@code key=value} lines in key order, with the header comment
     * {@link Properties#store} would normally emit stripped out.
     *
     * <p>A rule frozen without a {@link RuleDoc} keeps its description — a whole sentence, with
     * spaces and punctuation — as the index key. {@link Properties#load} treats an unescaped space in
     * a key as the key/value delimiter, so hand-building {@code "key=value"} strings truncates such a
     * key at its first word. Routing through a key-sorted {@link Properties#store} instead reuses its
     * escaping, so every key and value round-trips through {@link Properties#load} regardless of what
     * characters it contains.
     */
    private static List<String> sortedLines(Properties index) throws IOException {
        Properties sortedByKey = new Properties() {
            @Override
            public Set<Map.Entry<Object, Object>> entrySet() {
                return super.entrySet().stream()
                        .sorted(Comparator.comparing(entry -> (String) entry.getKey()))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }
        };
        sortedByKey.putAll(index);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sortedByKey.store(out, null);
        return out.toString(StandardCharsets.ISO_8859_1).lines()
                .filter(line -> !line.startsWith("#"))
                .toList();
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
