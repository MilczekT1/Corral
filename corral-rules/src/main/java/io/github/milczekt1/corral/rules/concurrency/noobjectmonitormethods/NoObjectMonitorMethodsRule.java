package io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.CodeUnitAccessTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may call or reference {@code Object.wait}, {@code notify}, {@code notifyAll} or
 * {@code TimeUnit.timedWait}, which calls {@code Object.wait} itself.
 *
 * <p>The {@code Object} methods are matched by name and exact parameter list: the five signatures
 * are {@code final} on {@code Object}, so nothing else can declare them. Do not match them on the
 * access's owner — javac 18+ records an interface-typed receiver's interface, such as
 * {@code java.util.Queue}, as the owner of an {@code Object} method called on it.
 * {@code LockSupport.park} and {@code Condition.await}/{@code signal} are not matched.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoObjectMonitorMethodsRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.concurrency.no-object-monitor-methods")
            .why("""
                    wait/notify is a hand-rolled condition variable. wait can wake spuriously, so \
                    without a loop re-checking the condition the thread acts on state that is not \
                    there. notify wakes one arbitrary waiter; if it cannot proceed, the signal is lost, \
                    and a notify sent before anyone waits is lost too. Either way the waiter hangs with \
                    no exception and no log line. java.util.concurrent has implemented these protocols \
                    correctly since Java 5, with timeouts.""")
            .howToFix("""
                    Name the coordination pattern and use the class that implements it. A handoff \
                    between producer and consumer is a BlockingQueue. "Wait until N things finish" is a \
                    CountDownLatch, or a CompletableFuture if a value comes back. Permits are a \
                    Semaphore. If the protocol genuinely needs a condition variable, use ReentrantLock \
                    with one newCondition() per condition, and still call await inside a while loop \
                    that re-checks the state, because Condition.await can wake spuriously too. Prefer \
                    the timed forms — await(timeout, unit), tryAcquire(timeout, unit), \
                    poll(timeout, unit), get(timeout, unit) — and check their result: a timeout comes \
                    back as false from await or tryAcquire and null from poll, but as a \
                    TimeoutException from Future.get.""")
            .howNotToFix("""
                    Do NOT hide the calls behind a hand-written monitor class, a WaitHelper or a \
                    base-class doWait(). The helper's own call is flagged once; freezing that line \
                    silences every caller, and every caller still hangs. Do NOT replace lock.wait(ms) \
                    with TimeUnit.timedWait(lock, timeout), or a call with a method reference such as \
                    lock::notifyAll: both reach the same Object methods and are flagged the same way. Do \
                    NOT switch to LockSupport.park/unpark or a Thread.sleep polling loop — another \
                    hand-rolled protocol, which this rule does not catch. Do NOT translate wait to \
                    Condition.await and notify to signal one for one where a BlockingQueue, \
                    CountDownLatch, CompletableFuture or Semaphore fits. This rule does not see \
                    Condition, so the translation passes whether or not it kept the while loop around \
                    await, and without that loop it has both bugs again: await wakes spuriously the \
                    same way, and a signal with no waiter is lost the same way.""")
            .build();

    private static final DescribedPredicate<JavaAccess<?>> MONITOR_ACCESS = describe(
            "target is Object.wait, notify, notifyAll or TimeUnit.timedWait",
            access -> access.getTarget() instanceof CodeUnitAccessTarget target
                    && (isObjectMonitorMethod(target) || isTimeUnitTimedWait(target)));

    /** Do not switch to {@code callMethodWhere}: it misses a {@code lock::notifyAll} reference. */
    static final ArchRule DEFINITION = noClasses().should().accessTargetWhere(MONITOR_ACCESS);

    @ArchTest
    public static final ArchRule rule = new NoObjectMonitorMethodsRule().guard();

    private static boolean isObjectMonitorMethod(CodeUnitAccessTarget target) {
        List<String> parameters = target.getRawParameterTypes().stream().map(JavaClass::getName).toList();
        return switch (target.getName()) {
            case "wait" -> List.of(List.of(), List.of("long"), List.of("long", "int")).contains(parameters);
            case "notify", "notifyAll" -> parameters.isEmpty();
            default -> false;
        };
    }

    private static boolean isTimeUnitTimedWait(CodeUnitAccessTarget target) {
        return target.getName().equals("timedWait") && target.getOwner().isEquivalentTo(TimeUnit.class);
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
