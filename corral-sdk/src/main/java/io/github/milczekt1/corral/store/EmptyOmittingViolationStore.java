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
 * a rule with zero violations.
 *
 * <p>Register it in {@code archunit.properties}:
 * <pre>{@code freeze.store=io.github.milczekt1.corral.store.EmptyOmittingViolationStore}</pre>
 *
 * <p>A clean rule stays frozen: {@code FreezingArchRule} keys on the index entry, not on the file.
 * Only a rule with no entry at all seeds and passes, so <strong>commit the {@code stored.rules}
 * line</strong> that appears when a rule is first frozen.
 *
 * <p>Needs a public no-arg constructor — ArchUnit instantiates it reflectively.
 */
public class EmptyOmittingViolationStore implements ViolationStore {

    /**
     * The delegate's index file: {@code <rule-description>=<file-name>}, an undocumented
     * {@link TextFileBasedViolationStore} format pinned by {@code storedRulesMapsRuleDescriptionToFileName}.
     */
    private static final String INDEX_FILE = "stored.rules";

    private static final String DEFAULT_STORE_PATH = "archunit_store";

    private final TextFileBasedViolationStore delegate =
            new TextFileBasedViolationStore(EmptyOmittingViolationStore::fileNameFor);

    private Path storePath;

    /**
     * Names a rule's file after its id; a description that is not an id keeps ArchUnit's UUID.
     *
     * <p>Gated on {@link RuleDoc#isIdWithinCaps} rather than {@link RuleDoc#isId}, which applies no
     * length or segment cap and so admits an unbounded file name.
     *
     * <p>Reached only for a rule with no index entry yet — an existing entry's file name is reused.
     */
    static String fileNameFor(String ruleDescription) {
        return RuleDoc.isIdWithinCaps(ruleDescription) ? ruleDescription : UUID.randomUUID().toString();
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

    /** Always delegates first: that call writes the index entry that keeps the rule frozen. */
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
     * <p>{@link java.util.Properties#store} sorts by key only from JDK 21; on 17-20 it writes an
     * unstable map iteration order.
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
     * Renders {@code index} as key-ordered {@code key=value} lines, without {@link Properties#store}'s
     * header comment.
     *
     * <p>Routed through {@code store} rather than hand-built: an index key can be a whole sentence,
     * and {@link Properties#load} reads an unescaped space in a key as the delimiter.
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
