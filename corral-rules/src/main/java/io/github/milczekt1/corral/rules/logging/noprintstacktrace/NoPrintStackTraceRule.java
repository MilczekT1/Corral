package io.github.milczekt1.corral.rules.logging.noprintstacktrace;

import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may call or reference {@code Throwable.printStackTrace}, in any overload.
 *
 * <p>The recorded owner is the receiver's declared type, such as {@code java.io.IOException}, so
 * matching it by assignability needs ArchUnit's default
 * {@code resolveMissingDependenciesFromClassPath=true}: with it off, an exception's hierarchy stops at
 * its first JDK or library superclass, the project's own exceptions included, and only a receiver
 * declared as {@code Throwable} is matched. A {@code printStackTrace} method a project declares on its
 * own exception type is matched too. {@code Thread.dumpStack} is not matched.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoPrintStackTraceRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.logging.no-print-stack-trace")
            .why("""
                    printStackTrace() writes to System.err, past the logging configuration: no level, no \
                    logger name, no MDC, nothing to filter, route or alert on. A log aggregator ingests the \
                    trace as one record per line, with nothing tying the lines to each other or to the \
                    request, and its order relative to the application's own log output is not guaranteed. \
                    The PrintStream and PrintWriter overloads write the same unstructured text to whatever \
                    they are handed, usually stdout or a buffer that is then logged as a plain string. \
                    The usual shape, catch (Exception e) { e.printStackTrace(); }, also drops the message \
                    saying what was being attempted.""")
            .howToFix("""
                    Log the throwable through the logger, passing it as the last argument so the framework \
                    formats it: log.error("Failed to settle order {}", orderId, e). The exception then \
                    arrives as one record, at a level someone can filter, with the message that says what \
                    was being attempted, which is the part a stack trace never contains. If the exception \
                    is not actually an error at this layer, rethrow it wrapped and let the caller that can \
                    decide do the logging; logging and rethrowing produces the same trace twice. If the \
                    trace is needed as text that does not go to a log, such as a stored failure record, \
                    render it with a helper such as commons-lang3's ExceptionUtils.getStackTrace(e), which \
                    keeps the Caused by and Suppressed sections.""")
            .howNotToFix("""
                    Do NOT swap it for e.printStackTrace(System.out), a PrintWriter overload or a \
                    Throwable::printStackTrace method reference: all are matched, and the output is no more \
                    managed. Do NOT replace it with log.error(e.getMessage()), which drops the frames and \
                    logs null for an exception without a message, or with log.error("" + e) or \
                    e.toString(), which keep only the type. Do NOT concatenate a rendered stack trace, such \
                    as ExceptionUtils.getStackTrace(e), into the message: pass the throwable and let the \
                    framework render it. Do NOT rebuild the trace by hand from getStackTrace(): it drops the \
                    Caused by and Suppressed sections. Do NOT swallow the exception. This rule catches only the first of \
                    these; the others pass the build and are still wrong.""")
            .build();

    /** Owner and name, not signature: every overload, on every exception type. */
    private static final DescribedPredicate<JavaAccess<?>> PRINT_STACK_TRACE =
            target(owner(assignableTo(Throwable.class)))
                    .and(target(name("printStackTrace")))
                    .as("target is Throwable.printStackTrace");

    /** Do not switch to {@code callMethodWhere}: it misses a {@code Throwable::printStackTrace} reference. */
    static final ArchRule DEFINITION = noClasses().should().accessTargetWhere(PRINT_STACK_TRACE);

    @ArchTest
    public static final ArchRule rule = new NoPrintStackTraceRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
