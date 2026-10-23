package io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.domain.properties.HasParameterTypes;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import java.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No production class may call or reference the no-argument {@code get()} of a type assignable to
 * {@code Future}, except a {@code Future} implementation's own no-argument {@code get()} delegating to
 * another.
 *
 * <p>{@code join()}, library helpers that call {@code get()} for you, and other unbounded waits pass
 * this rule. It needs ArchUnit's default {@code resolveMissingDependenciesFromClassPath=true}: with it
 * off, {@code CompletableFuture}, {@code FutureTask}, {@code ForkJoinTask} and other JDK or library
 * subtypes are stubs, and only a call on {@code Future} itself or on a project's own implementation is
 * reported.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoFutureGetWithoutTimeoutRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.concurrency.no-future-get-without-timeout")
            .why("""
                    future.get() with no timeout waits until the task completes, with no upper bound. If \
                    the task is an HTTP call with no read timeout, or is stuck in a saturated pool's \
                    queue, the calling thread waits forever. That thread is usually a request thread, so \
                    the container's pool fills with threads parked on the same line, and a slow \
                    dependency becomes an application that is down. If a pool's tasks wait on other \
                    tasks in the same pool, the pool deadlocks. A timeout does not fix the dependency, \
                    but it turns an unbounded hang into a TimeoutException at a line that says what was \
                    being waited on.""")
            .howToFix("""
                    Call get(timeout, unit) with a timeout taken from the caller's budget. On \
                    TimeoutException, call future.cancel(true): it interrupts a task running on an \
                    executor thread, but not a CompletableFuture's task, and a task blocked in \
                    non-interruptible I/O keeps running, so give that call its own timeout too. Then fall \
                    back or fail with a status the caller can act on. If the future is already complete \
                    (invokeAll results, after isDone()), call get(0, TimeUnit.SECONDS): it returns at \
                    once, keeps a failed task's ExecutionException, and throws TimeoutException if the \
                    future was not complete after all. resultNow() (Java 19+) also reads it, but throws \
                    IllegalStateException without the cause for a failed or cancelled task, so call it \
                    only after state() returns SUCCESS. On a CompletableFuture, prefer composing with \
                    thenApply or thenCompose and returning the future, bounded with orTimeout or \
                    completeOnTimeout (Java 9+) so no thread waits. orTimeout(...).get() is still \
                    flagged: the rule cannot see a bound set upstream. Where the value must be read on \
                    this thread, call get(timeout, unit) itself. If the wait is meant to last as long \
                    as the work does — a startup readiness check, a shutdown drain, a batch job run to \
                    completion — pass the limit you would actually tolerate, from configuration: the \
                    readiness timeout, the shutdown grace period, a job timeout of hours. A named limit \
                    still turns a hung task into an error at a line that says what it waited on; \
                    Long.MAX_VALUE never does. A codebase that is wholly batch processing can exclude \
                    the rule instead. A Future implementation's own get() may delegate to a wrapped \
                    future's get(): that call is not flagged. A method reference such as future::get \
                    counts as a call. Only Future.get() is checked: \
                    invokeAll and invokeAny without a timeout, CountDownLatch.await(), \
                    BlockingQueue.take() and other waits are not.""")
            .howNotToFix("""
                    Do NOT switch to join(), getNow(...) or get(Long.MAX_VALUE, ...): this rule catches \
                    none of them; a join() not preceded by orTimeout or completeOnTimeout still waits \
                    forever, and getNow invents a result. Do NOT wrap the call in a helper such as \
                    FutureUtils.await(future): the helper's own get() is flagged once, freezing that \
                    line silences every caller, and every caller still hangs. A library helper such as \
                    Guava's Uninterruptibles.getUninterruptibly(future) or Futures.getUnchecked(future) \
                    is not flagged at all, and hangs the same way. Do NOT wrap the future in a Future \
                    of your own to move the call into its get(): that delegation is not flagged, and \
                    every call to the wrapper's get() is. Do NOT try to move the class out of \
                    scope by naming it like a test: scope is decided by output location and declared \
                    test methods. Do NOT add a JUnit test method to a production class: a class \
                    declaring one counts as a test class, so this is not caught.""")
            .build();

    private static final DescribedPredicate<JavaAccess<?>> UNBOUNDED_GET = describe(
            "target is Future.get() with no timeout, outside a Future's own get()",
            access -> access.getTarget() instanceof CodeUnitAccessTarget target
                    && isNoArgumentFutureGet(target)
                    && !isNoArgumentFutureGet(access.getOrigin()));

    /** Do not switch to {@code callMethodWhere}: it misses a {@code future::get} reference. */
    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.PRODUCTION_CLASSES)
            .should().accessTargetWhere(UNBOUNDED_GET);

    @ArchTest
    public static final ArchRule rule = new NoFutureGetWithoutTimeoutRule().guard();

    private static <T extends HasName & HasParameterTypes & HasOwner<JavaClass>> boolean isNoArgumentFutureGet(
            T codeUnit) {
        return codeUnit.getName().equals("get")
                && codeUnit.getRawParameterTypes().isEmpty()
                && codeUnit.getOwner().isAssignableTo(Future.class);
    }

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
