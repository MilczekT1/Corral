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
 * <p><strong>The index entry is deliberately kept.</strong> {@code FreezingArchRule} treats a rule
 * the store does not contain as one to seed — it records whatever violations exist and passes. Drop
 * a clean rule's entry and its first real violation would be absorbed as debt instead of failing.
 * Only the (empty) file is removed.
 *
 * <p>Must have a public no-arg constructor: ArchUnit instantiates it reflectively.
 */
public class EmptyOmittingViolationStore implements ViolationStore {

    private final TextFileBasedViolationStore delegate = new TextFileBasedViolationStore();

    private Path storePath;

    @Override
    public void initialize(Properties properties) {
        delegate.initialize(properties);
        storePath = Path.of(properties.getProperty("default.path"));
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
