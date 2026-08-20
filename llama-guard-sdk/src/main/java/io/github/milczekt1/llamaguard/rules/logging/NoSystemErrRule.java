package io.github.milczekt1.llamaguard.rules.logging;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.llamaguard.DocumentedRule;
import io.github.milczekt1.llamaguard.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may write to {@code System.err}.
 *
 * <p>Sibling of {@link NoSystemOutRule}, kept separate rather than folded into one {@code orShould}
 * chain: separate ids are separate freeze-store keys, so a consumer carries stdout debt and stderr
 * debt independently, and each doc argues about one stream. Matches the field access, so every
 * method on the stream counts.
 *
 * <p>One thing it cannot see: {@code throwable.printStackTrace()} reaches System.err from inside
 * {@code java.lang.Throwable}, so the calling class never touches the field. The doc still argues
 * against it, because whoever reads a failure here is usually one step away from writing it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoSystemErrRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("logging.no-system-err")
            .why("""
                    System.err is the worse of the two streams to write to: it is where stack traces get \
                    dumped, so the failures most worth alerting on are exactly the ones that end up outside \
                    the log pipeline — no level, no correlation id, no stack-trace-aware formatting, nothing \
                    for an alert to match on. Interleaved with stdout by whatever collects the process \
                    output, it is not even reliably readable in order.""")
            .howToFix("""
                    Log it: log.error("what failed and in which operation", exception), passing the exception \
                    as the last argument so the logger formats the stack trace itself, rather than calling \
                    printStackTrace() or printing the exception by hand. If the failure is recoverable and \
                    routine, log.warn says so more accurately than a stack trace on stderr ever did.""")
            .howNotToFix("""
                    Do NOT swap System.err for System.out to satisfy this rule — the sibling rule \
                    logging.no-system-out catches that too, and it only makes the failure harder to find. \
                    Do NOT swallow the exception, or narrow the catch until it disappears, just to remove \
                    the print: the print is the wrong destination, not the wrong intent.""")
            .build();

    static final ArchRule RULE = noClasses()
            .should().accessField(System.class, "err");

    @ArchTest
    public static final ArchRule rule = new NoSystemErrRule().guard();

    @Override
    public ArchRule definition() {
        return RULE;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
