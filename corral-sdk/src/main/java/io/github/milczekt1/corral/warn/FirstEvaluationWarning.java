package io.github.milczekt1.corral.warn;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Prints run-wide warnings once, when the first wrapped rule is evaluated, then delegates.
 *
 * <p>The messages are computed then, not at wrap time: rules register during class initialisation,
 * so only by the first evaluation has discovery registered every rule this run will see. Assumes one
 * JVM and one discovery pass — under {@code forkCount > 1} or a sharded CI a message can name
 * something another fork registered.
 *
 * <p>Every method delegates: this rides along with the rule it wraps rather than standing in for it.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class FirstEvaluationWarning implements ArchRule {

    /** Shared by every rule wrapped through {@link #wrap(ArchRule, List)}: once per run, not per rule. */
    private static final AtomicBoolean WARNED_THIS_RUN = new AtomicBoolean(false);

    private static final String PREFIX = "WARNING: Corral — ";

    private final ArchRule delegate;
    private final AtomicBoolean warned;
    private final List<Supplier<String>> messages;
    private final Consumer<String> sink;

    /** @param messages each returns its warning, or {@code null} when it has nothing to say */
    public static ArchRule wrap(ArchRule rule, List<Supplier<String>> messages) {
        return wrap(rule, WARNED_THIS_RUN, messages, FirstEvaluationWarning::printWarning);
    }

    static ArchRule wrap(
            ArchRule rule, AtomicBoolean warned, List<Supplier<String>> messages, Consumer<String> sink) {
        return new FirstEvaluationWarning(rule, warned, List.copyOf(messages), sink);
    }

    /**
     * {@link System#err}, not SLF4J: a consumer with no logging provider would never see this. The
     * prefix stands in for a log level, which Maven and Gradle do not add to a test JVM's stderr.
     */
    @SuppressWarnings("java:S106")
    static void printWarning(String message) {
        System.err.println(PREFIX + message);
    }

    @Override
    public void check(JavaClasses classes) {
        warnOnce();
        delegate.check(classes);
    }

    @Override
    public EvaluationResult evaluate(JavaClasses classes) {
        warnOnce();
        return delegate.evaluate(classes);
    }

    private void warnOnce() {
        if (!warned.compareAndSet(false, true)) {
            return;
        }
        messages.stream()
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .forEach(sink);
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public ArchRule as(String newDescription) {
        return new FirstEvaluationWarning(delegate.as(newDescription), warned, messages, sink);
    }

    @Override
    public ArchRule because(String reason) {
        return new FirstEvaluationWarning(delegate.because(reason), warned, messages, sink);
    }

    @Override
    public ArchRule allowEmptyShould(boolean allowEmptyShould) {
        return new FirstEvaluationWarning(delegate.allowEmptyShould(allowEmptyShould), warned, messages, sink);
    }
}
