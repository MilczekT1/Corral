package io.github.milczekt1.corral.rules.logging;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may write to {@code System.out}.
 *
 * <p>Matches the <em>field access</em> rather than one method call, so every use of the stream
 * counts, static initializers included.
 *
 * <p>Applies to every imported class, tests included.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoSystemOutRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.logging.no-system-out")
            .why("""
                    A write to System.out bypasses the logging configuration entirely: no level, no logger \
                    name, no structured context, no appender. It cannot be filtered, routed, shipped to an \
                    aggregator or silenced, and once output is collected centrally the line arrives detached \
                    from the request it belongs to — visible on a developer's console and effectively \
                    invisible everywhere else.""")
            .howToFix("""
                    Log the message through the project's logger, at the level it deserves — log.debug for a \
                    trace someone enables while investigating, log.info for an event worth keeping. The \
                    message then carries its logger name and level, and inherits whatever context and \
                    formatting the appenders add.""")
            .howNotToFix("""
                    Do NOT route the same write through a wrapper to dodge the field match — a PrintStream \
                    local, a Console helper, a stream fetched reflectively — the output stays just as \
                    unmanaged and is now harder to find. Do NOT delete a genuinely useful diagnostic instead \
                    of logging it: the message was worth writing, it was written to the wrong place.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .should().accessField(System.class, "out");

    @ArchTest
    public static final ArchRule rule = new NoSystemOutRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
