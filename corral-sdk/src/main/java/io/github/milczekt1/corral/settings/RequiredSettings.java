package io.github.milczekt1.corral.settings;

import static java.util.stream.Collectors.joining;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.doc.RuleRegistry;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/** Fails a rule whose {@link RuleDoc#requiredSettings()} are missing from {@value CorralSettings#FILE}. */
@UtilityClass
public class RequiredSettings {

    /**
     * {@code rule} unchanged when every declared setting has a value, or when the file is broken —
     * {@code RuleExclusions} fails every rule for that. Apply outside {@code FreezingArchRule}.
     */
    public static ArchRule applyTo(ArchRule rule, RuleDoc doc) {
        return applyTo(rule, doc, CorralSettings.current());
    }

    static ArchRule applyTo(ArchRule rule, RuleDoc doc, SettingsFile settings) {
        if (settings.isBroken()) {
            return rule;
        }
        List<String> missing = doc.requiredSettings().stream()
                .filter(key -> settings.value(key).isEmpty())
                .toList();
        return missing.isEmpty() ? rule : new Unconfigured(rule.getDescription(), missingMessage(missing));
    }

    /**
     * The non-{@code corral.} keys no registered rule declares, or {@code null} when there are none.
     * Live at call time: rules are still registering while their classes initialise.
     */
    public static String undeclaredKeysWarning() {
        return undeclaredKeysWarning(CorralSettings.current(), RuleRegistry.all());
    }

    static String undeclaredKeysWarning(SettingsFile settings, Collection<RuleDoc> docs) {
        Set<String> declared = docs.stream()
                .flatMap(doc -> doc.requiredSettings().stream())
                .collect(Collectors.toSet());
        List<String> undeclared = settings.values().keySet().stream()
                .filter(key -> !key.startsWith("corral."))
                .filter(key -> !declared.contains(key))
                .toList();

        if (undeclared.isEmpty()) {
            return null;
        }
        return """
                %s: %d setting%s no rule in this run declares:
                %s
                If you ran a single rule this is expected. In a full run it means the key is read by \
                nothing — a typo, or a rule that no longer needs it.\
                """.formatted(CorralSettings.FILE, undeclared.size(), undeclared.size() == 1 ? "" : "s",
                        undeclared.stream().map(key -> "  " + key).collect(joining(System.lineSeparator())));
    }

    private static String missingMessage(List<String> missing) {
        return missing.stream().map(CorralSettings::missing).collect(joining(System.lineSeparator()));
    }

    /** Nothing a caller chains softens it. */
    @RequiredArgsConstructor
    private static final class Unconfigured implements ArchRule {

        private final String description;
        private final String message;

        @Override
        public void check(JavaClasses classes) {
            throw new AssertionError(message);
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            throw new AssertionError(message);
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public ArchRule as(String newDescription) {
            return new Unconfigured(newDescription, message);
        }

        @Override
        public ArchRule because(String reason) {
            return this;
        }

        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return this;
        }
    }
}
