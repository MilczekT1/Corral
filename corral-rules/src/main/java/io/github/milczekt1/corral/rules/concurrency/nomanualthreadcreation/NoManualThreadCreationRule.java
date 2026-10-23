package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorReferenceTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import java.util.Timer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No production class may call or reference a constructor of a type assignable to {@code Thread} or
 * {@code java.util.Timer}.
 *
 * <p>A subclass is reported where it calls its parent's constructor and again wherever it is
 * constructed. {@code Thread.Builder}, {@code Thread.startVirtualThread} and a call to
 * {@code ThreadFactory.newThread} pass this rule; a {@code ThreadFactory} implementation calling
 * {@code new Thread} does not. It needs ArchUnit's default
 * {@code resolveMissingDependenciesFromClassPath=true}: with it off, JDK and library subclasses of
 * either type are stubs, so a call to one's constructor is missed, and so is a project class
 * extending one, both at its super call and at every {@code new} of it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoManualThreadCreationRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.concurrency.no-manual-thread-creation")
            .why("""
                    new Thread(...) is an unbounded resource allocation that looks like an object creation. Nothing \
                    caps it: a thread per request survives a load test at ten users and fails at two thousand with \
                    OutOfMemoryError: unable to create native thread. Nothing names it unless every call site \
                    remembers to: the thread dump taken during that incident is a wall of Thread-247. Nothing queues \
                    it: with no backpressure and no rejection policy the system degrades by exhausting the machine \
                    instead of refusing work. Nothing stops it: a non-daemon thread no shutdown path knows about keeps \
                    the JVM alive or leaks across a redeploy. A java.util.Timer is the same thing: each one starts its \
                    own thread, non-daemon by default, and one exception thrown from a task kills that thread, so \
                    every task still scheduled on it never runs.""")
            .howToFix("""
                    Submit the task to an ExecutorService the application creates once, configures explicitly and \
                    shuts down on the way out, with a capped thread count, a bounded queue, a rejection policy chosen \
                    on purpose and named threads. Take the names from a factory that constructs the thread for you: \
                    Thread.ofPlatform().name("orders-", 0).factory() on Java 21+, Guava's ThreadFactoryBuilder, \
                    Spring's CustomizableThreadFactory, or, on Java 17 without either library, a ThreadFactory that \
                    takes its thread from Executors.defaultThreadFactory() and sets its name. For delayed or periodic \
                    work, use a ScheduledExecutorService, not a Timer. For fan-out, pass the executor to \
                    CompletableFuture.supplyAsync(task, executor). For a thread per task on Java 21+, use \
                    Executors.newVirtualThreadPerTaskExecutor(). A shutdown hook needs a Thread, because \
                    Runtime.addShutdownHook takes nothing else: freeze it as debt. new Thread(hook) is one line; an \
                    anonymous new Thread() { ... } is two.""")
            .howNotToFix("""
                    Do NOT move new Thread(...) or new Timer() into a helper, a static factory or a hand-written \
                    ThreadFactory, and do NOT pass Thread::new as the factory: each is flagged, and each is the same \
                    unmanaged thread. Do NOT extend Thread or Timer instead: the subclass's constructor is flagged, \
                    and so is every call to it. Do NOT start the thread through a JDK factory instead: \
                    Thread.ofPlatform().start(task), Thread.startVirtualThread(task), \
                    Executors.defaultThreadFactory().newThread(task) with nothing owning the thread afterwards, or a \
                    fresh Executors.newSingleThreadExecutor() per call. This rule does not catch these, and each is \
                    the same unmanaged thread; the last one also leaks a pool. Do NOT replace new Thread(task).start() \
                    with Executors.newCachedThreadPool(): its thread count is uncapped, so a burst still ends in \
                    unable to create native thread, and this rule does not catch it. Do NOT drop the executor from \
                    CompletableFuture.runAsync or supplyAsync: this rule does not catch that either, and the task \
                    lands on the JVM-wide common pool. Do NOT try to move the class out of scope by naming it like a \
                    test: scope is decided by output location and declared test methods; a helper in a test-support \
                    module's src/main, or in Gradle testFixtures or integrationTest output, is production by that \
                    measure and is flagged. Do NOT add a JUnit test method to a production class: a class declaring \
                    one counts as a test class, so this is not caught.""")
            .build();

    private static final DescribedPredicate<JavaAccess<?>> THREAD_OR_TIMER_CONSTRUCTION = describe(
            "target is a constructor of Thread, java.util.Timer or a subclass of either",
            access -> isConstructor(access.getTarget()) && isThreadOrTimer(access.getTargetOwner()));

    /** Do not switch to {@code callConstructorWhere}: it misses a {@code Thread::new} reference. */
    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.PRODUCTION_CLASSES)
            .should().accessTargetWhere(THREAD_OR_TIMER_CONSTRUCTION);

    @ArchTest
    public static final ArchRule rule = new NoManualThreadCreationRule().guard();

    private static boolean isConstructor(AccessTarget target) {
        return target instanceof ConstructorCallTarget || target instanceof ConstructorReferenceTarget;
    }

    private static boolean isThreadOrTimer(JavaClass owner) {
        return owner.isAssignableTo(Thread.class) || owner.isAssignableTo(Timer.class);
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
