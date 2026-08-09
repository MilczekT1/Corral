package io.github.milczekt1.llamarules.rules.freeze;

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

    private final TextFileBasedViolationStore delegate = new TextFileBasedViolationStore();

    private Path storePath;

    @Override
    public void initialize(Properties properties) {
        delegate.initialize(properties);
        // Mirrors TextFileBasedViolationStore's private STORE_PATH_DEFAULT, so omitting default.path
        // lands in the delegate's own directory rather than an NPE. Recheck on ArchUnit upgrades.
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

    /** Absent file means zero known violations — the clean-rule case, not a missing store. */
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
     * The file the delegate stores this rule's violations in, or empty when it has no index entry.
     *
     * <p>Reads {@code stored.rules}, whose {@code <rule-description>=<uuid>} layout is
     * {@link TextFileBasedViolationStore}'s undocumented internal format.
     * {@code storedRulesMapsRuleDescriptionToFileName} pins it, so an upgrade that changes it fails
     * loudly rather than mishandling a consumer's store.
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
