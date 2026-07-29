package io.github.milczekt1.archrules.format;

import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.lang.FailureDisplayFormat;
import com.tngtech.archunit.lang.FailureMessages;
import com.tngtech.archunit.lang.Priority;
import io.github.milczekt1.archrules.RuleDoc;
import io.github.milczekt1.archrules.RuleRegistry;
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
        String description = describeSafely(rule);
        String countInfo = countInfoSafely(failureMessages);
        List<String> lines = failureMessages == null ? List.of() : List.copyOf(failureMessages);
        try {
            Optional<RuleDoc> doc = RuleRegistry.find(description);
            return doc.isPresent()
                    ? render(doc.get(), lines, priority)
                    : defaultFormat(description, lines, countInfo, priority);
        } catch (RuntimeException e) {
            return defaultFormat(description, lines, countInfo, priority);
        }
    }

    /** Package-private seam: rendering a documented rule, testable with a plain list. */
    String render(RuleDoc doc, List<String> violationLines, Priority priority) {
        String nl = System.lineSeparator();
        StringBuilder out = new StringBuilder();

        out.append("Architecture Violation [").append(doc.id()).append(']')
                .append(" [Priority: ").append(priority.asString()).append(']').append(nl).append(nl);

        out.append("WHY:").append(nl).append(indent(doc.why())).append(nl).append(nl);
        out.append("HOW TO FIX:").append(nl).append(indent(doc.howToFix())).append(nl).append(nl);

        doc.howNotToFix().ifPresent(text ->
                out.append("HOW NOT TO FIX (this rule):").append(nl).append(indent(text)).append(nl).append(nl));

        out.append("HOW NOT TO FIX (always):").append(nl);
        for (String clause : AntiFixPolicy.clauses()) {
            out.append(INDENT).append("- ").append(clause).append(nl);
        }
        out.append(nl);

        out.append("Offending locations:").append(nl);
        for (String line : violationLines) {
            out.append(INDENT).append(line).append(nl);
        }
        return out.toString();
    }

    /** Package-private seam: byte-for-byte ArchUnit's default rendering, so foreign rules look untouched. */
    String defaultFormat(String description, List<String> violationLines, String countInfo, Priority priority) {
        String violationTexts = String.join(System.lineSeparator(), violationLines);
        return String.format("Architecture Violation [Priority: %s] - Rule '%s' was violated (%s):%n%s",
                priority.asString(), description, countInfo, violationTexts);
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

    private static String indent(String text) {
        return text.lines()
                .map(line -> INDENT + line)
                .reduce((a, b) -> a + System.lineSeparator() + b)
                .orElse("");
    }
}
