package io.github.milczekt1.archrules.freeze;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.TextFileBasedViolationStore;
import com.tngtech.archunit.library.freeze.ViolationStore;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * A {@link ViolationStore} that keeps the {@code stored.rules} index complete but writes no file for
 * a rule with zero violations, so a committed freeze store carries no empty files.
 *
 * <p>Register it in {@code archunit.properties}:
 * <pre>{@code freeze.store=io.github.milczekt1.archrules.freeze.EmptyOmittingViolationStore}</pre>
 *
 * <p><strong>A clean rule stays frozen, and its first later violation fails the build.</strong> Add a
 * rule while the code complies, and this store records an index entry for it with no violation file.
 * Months later, when someone violates it for the first time, that violation is new and the build
 * <strong>fails</strong> — it is not absorbed as debt.
 *
 * <p>That works because {@code FreezingArchRule} decides via {@link #contains}, which keys on the
 * {@code stored.rules} <em>index entry</em> — not on the presence of a violation file. "No file"
 * means "zero known violations"; it does not mean "unknown rule". Only a rule with no index entry at
 * all seeds and passes, which is why the entry is deliberately kept and only the empty file removed.
 *
 * <p><strong>The one way to lose that guarantee is process, not code:</strong> the run that first
 * freezes a new rule appends its line to {@code stored.rules}. Commit that change. Leave it
 * uncommitted and the rule has no entry in CI, so its first violation is seeded as debt and the
 * build stays green — a rule that looks armed and is not.
 *
 * <p>Must have a public no-arg constructor: ArchUnit instantiates it reflectively.
 */
public class EmptyOmittingViolationStore implements ViolationStore {

    private final TextFileBasedViolationStore delegate = new TextFileBasedViolationStore();

    private Path storePath;

    @Override
    public void initialize(Properties properties) {
        delegate.initialize(properties);
        // "archunit_store" mirrors TextFileBasedViolationStore's own private STORE_PATH_DEFAULT
        // constant, so a consumer who omits default.path gets the same directory the delegate
        // itself would use, not an NPE. Check that constant if a future ArchUnit upgrade changes it.
        storePath = Path.of(properties.getProperty("default.path", "archunit_store"));
    }

    @Override
    public boolean contains(ArchRule rule) {
        return delegate.contains(rule);
    }

    @Override
    public void save(ArchRule rule, List<String> violations) {
        delegate.save(rule, violations);
        if (violations.isEmpty()) {
            deleteViolationFile(rule);
        }
    }

    /**
     * An indexed rule whose file is absent has zero known violations — that is the clean-rule case
     * this store creates, not a missing store. The rule remains {@link #contains contained}, so any
     * violation found later is new and fails.
     */
    @Override
    public List<String> getViolations(ArchRule rule) {
        return violationFile(rule).filter(Files::exists).isPresent()
                ? delegate.getViolations(rule)
                : List.of();
    }

    private void deleteViolationFile(ArchRule rule) {
        violationFile(rule).ifPresent(file -> {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Could not remove the empty violation file for rule '"
                                + rule.getDescription() + "'", e);
            }
        });
    }

    /**
     * Resolves a rule to the file the delegate stores its violations in.
     *
     * <p><strong>Accepted coupling:</strong> this reads {@code stored.rules}, whose
     * {@code <rule-description>=<uuid>} layout is an implementation detail of
     * {@link TextFileBasedViolationStore}. That class is public, but the file format is not a
     * documented contract, so an ArchUnit upgrade could change it —
     * {@code storedRulesMapsRuleDescriptionToFileName} pins the assumption so such a change fails
     * loudly instead of silently mishandling a consumer's store.
     *
     * @return empty when the rule has no index entry yet
     */
    private Optional<Path> violationFile(ArchRule rule) {
        Path index = storePath.resolve("stored.rules");
        if (!Files.exists(index)) {
            return Optional.empty();
        }
        Properties storedRules = new Properties();
        try (InputStream in = Files.newInputStream(index)) {
            storedRules.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + index, e);
        }
        return Optional.ofNullable(storedRules.getProperty(rule.getDescription()))
                .map(storePath::resolve);
    }
}
