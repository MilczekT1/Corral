package io.github.milczekt1.archrules.format;

import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.lang.FailureDisplayFormat;
import com.tngtech.archunit.lang.FailureMessages;
import com.tngtech.archunit.lang.Priority;
import io.github.milczekt1.archrules.RuleDoc;
import io.github.milczekt1.archrules.RuleRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Renders framework rule failures as agent- and human-readable guidance: WHY the rule exists,
 * HOW TO FIX it, the rule's own anti-fix trap, and the global {@link AntiFixPolicy}.
 *
 * <p>Register it in a consumer's {@code src/test/resources/archunit.properties}:
 * <pre>{@code failureDisplayFormat=io.github.milczekt1.archrules.format.AgentFriendlyFailureDisplayFormat}</pre>
 *
 * <p>{@code failureDisplayFormat} is a <strong>global per-run</strong> setting, so this formatter
 * also sees the consumer's own unrelated rules. Any description that is not a registered
 * {@link RuleDoc} id falls through to ArchUnit's standard rendering, and this class never throws:
 * a formatter must never mask a real architecture violation with its own stack trace.
 *
 * <p>Must stay {@code public} with a {@code public} no-arg constructor — ArchUnit instantiates it
 * reflectively from the configured class name.
 */
public class AgentFriendlyFailureDisplayFormat implements FailureDisplayFormat {

    private static final String INDENT = "  ";
    private static final String UNKNOWN_RULE = "<unknown rule>";

    @Override
    public String formatFailure(HasDescription rule, FailureMessages failureMessages, Priority priority) {
        try {
            return formatFailureUnsafe(rule, failureMessages, priority);
        } catch (RuntimeException e) {
            return lastResortFormat(rule, failureMessages, priority);
        }
    }

    private String formatFailureUnsafe(HasDescription rule, FailureMessages failureMessages, Priority priority) {
        String description = describeSafely(rule);
        String countInfo = countInfoSafely(failureMessages);
        List<String> lines = linesSafely(failureMessages);
        try {
            Optional<RuleDoc> doc = RuleRegistry.find(description);
            return doc.isPresent()
                    ? render(doc.get(), lines, priority)
                    : defaultFormat(description, lines, countInfo, priority);
        } catch (RuntimeException e) {
            return defaultFormat(description, lines, countInfo, priority);
        }
    }

    /**
     * Provably non-throwing backstop for {@link #formatFailure}, reached only if every guarded
     * path above still failed — e.g. a corrupted {@link RuleDoc}, or {@link #defaultFormat} itself
     * throwing on a null {@link Priority} while it was already handling another exception. Every
     * value here comes from {@code String.valueOf} or {@code getClass().getName()} — never from a
     * caller-controlled accessor or {@code toString()} that could itself throw or be null. Package
     * -private so the guarantee is directly testable without needing a real {@link FailureMessages}.
     */
    String lastResortFormat(HasDescription rule, FailureMessages failureMessages, Priority priority) {
        return "Architecture Violation [Priority: " + String.valueOf(priority) + "] - " + UNKNOWN_RULE
                + " (failed to render failure details; rule type=" + safeClassName(rule)
                + ", failureMessages type=" + safeClassName(failureMessages) + ")";
    }

    /** Package-private seam: rendering a documented rule, testable with a plain list. */
    String render(RuleDoc doc, List<String> violationLines, Priority priority) {
        try {
            String nl = System.lineSeparator();
            List<String> lines = violationLines == null ? List.of() : violationLines;
            String priorityLabel = priority == null ? "UNKNOWN" : priority.asString();
            StringBuilder out = new StringBuilder();

            out.append("Architecture Violation [").append(doc.id()).append(']')
                    .append(" [Priority: ").append(priorityLabel).append(']').append(nl).append(nl);

            out.append("WHY:").append(nl).append(indent(doc.why())).append(nl).append(nl);
            out.append("HOW TO FIX:").append(nl).append(indent(doc.howToFix())).append(nl).append(nl);

            doc.howNotToFix().ifPresent(text -> out.append("HOW NOT TO FIX (this rule):").append(nl)
                    .append(indent(text)).append(nl).append(nl));

            out.append("HOW NOT TO FIX (always):").append(nl);
            for (String clause : AntiFixPolicy.clauses()) {
                out.append(INDENT).append("- ").append(clause).append(nl);
            }
            out.append(nl);

            out.append("Offending locations:").append(nl);
            for (String line : lines) {
                out.append(INDENT).append(line).append(nl);
            }
            return out.toString();
        } catch (RuntimeException e) {
            return lastResortFormat(null, null, priority);
        }
    }

    /** Package-private seam: byte-for-byte ArchUnit's default rendering, so foreign rules look untouched. */
    String defaultFormat(String description, List<String> violationLines, String countInfo, Priority priority) {
        try {
            List<String> lines = violationLines == null ? List.of() : violationLines;
            String violationTexts = String.join(System.lineSeparator(), lines);
            String priorityLabel = priority == null ? "UNKNOWN" : priority.asString();
            return String.format("Architecture Violation [Priority: %s] - Rule '%s' was violated (%s):%n%s",
                    priorityLabel, description, countInfo, violationTexts);
        } catch (RuntimeException e) {
            return lastResortFormat(null, null, priority);
        }
    }

    /** Package-private seam: a hostile or half-built rule must not break failure reporting. */
    String describeSafely(HasDescription rule) {
        try {
            String description = rule == null ? null : rule.getDescription();
            return description == null ? UNKNOWN_RULE : description;
        } catch (RuntimeException e) {
            return UNKNOWN_RULE;
        }
    }

    private static String countInfoSafely(FailureMessages messages) {
        try {
            return messages == null ? "0 times" : messages.getInformationAboutNumberOfViolations();
        } catch (RuntimeException e) {
            return "unknown number of times";
        }
    }

    /** Guards against a null {@code FailureMessages}, a throwing iterator, and null elements. */
    private static List<String> linesSafely(FailureMessages messages) {
        if (messages == null) {
            return List.of();
        }
        try {
            List<String> copy = new ArrayList<>();
            for (String line : messages) {
                copy.add(line == null ? "" : line);
            }
            return List.copyOf(copy);
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /** {@code getClass()} is final and {@code getName()} runs no caller code, so this cannot throw. */
    private static String safeClassName(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static String indent(String text) {
        return text.lines()
                .map(line -> INDENT + line)
                .reduce((a, b) -> a + System.lineSeparator() + b)
                .orElse("");
    }
}
