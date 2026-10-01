package io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.Set;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No {@code @Async} method may be {@code private}, {@code static}, {@code final}, or declared in a
 * {@code final} class.
 *
 * <p>A method is async when it is meta-annotated {@code @Async} or — unless it is private or static —
 * when its class or any superclass is. Matched by annotation name, so a consumer without
 * {@code spring-context} still runs the rule. Synthetic methods, bridge methods included, are never
 * reported.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoAsyncOnUnproxyableMethodRule implements DocumentedRule {

    private static final String ASYNC = "org.springframework.scheduling.annotation.Async";

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.no-async-on-unproxyable-method")
            .why("""
                    @Async is proxy-based advice: AsyncAnnotationBeanPostProcessor wraps the bean, and \
                    Spring Boot builds that wrapper with CGLIB by default — a runtime subclass that \
                    overrides each advised method and hands the invocation to a TaskExecutor. Unlike \
                    @Transactional before Spring 6, @Async has no public-only filter, so a protected or \
                    package-private @Async method on a class-based proxy does run asynchronously. What \
                    the proxy cannot reach is whatever it cannot override: a private, final or static \
                    method runs straight through on the target, synchronously, on the caller's thread. \
                    Nothing throws and Spring logs nothing above DEBUG; the work still happens, it just \
                    blocks the thread it was meant to free, and the symptom is a latency regression \
                    under load with nothing in the diff to point at. A final class cannot be subclassed \
                    at all, so startup fails with "Cannot subclass final class" — loud, but only once \
                    that bean is wired, which may be one profile or one integration test away. A \
                    class-level @Async reaches every instance method, so marking one of them final \
                    opens the same hole with no annotation next to it to prompt a second look.""")
            .howToFix("""
                    For a final method or a final class, remove final. If the class is final for a \
                    reason, it is not the place for asynchronous work: move the @Async method onto a \
                    small bean that may be proxied and inject it where the work is triggered. For a \
                    private or static method, make it a non-private instance method on a bean and call \
                    it through the injected reference — an @Async method called from another method of \
                    the same class bypasses the proxy whatever its modifiers. Check the return type \
                    while you are there: @Async supports void, Future and CompletableFuture, and any \
                    other return value is discarded.""")
            .howNotToFix("""
                    Do NOT delete the annotation "because it did nothing anyway" — the intent was to \
                    get work off the calling thread, and dropping it makes the synchronous behaviour \
                    permanent instead of accidental. Do NOT widen a private method and keep calling it \
                    from inside the same class: the call never leaves the target object, so it is still \
                    synchronous, and this check reads modifiers, not call sites, so it would go quiet \
                    without anything being fixed. Do NOT switch to AspectJ weaving \
                    (AdviceMode.ASPECTJ) to make the premise go away; that is a new runtime concern \
                    adopted to silence one check. Do NOT set spring.aop.proxy-target-class=false to get \
                    JDK dynamic proxies: those advise only methods declared on an interface, and break \
                    every injection point typed by the concrete class. Do NOT hand-roll a new \
                    Thread(...) or an unbounded executor to get the concurrency back outside the \
                    managed pool. Not caught: an @Async declared only on an interface the class \
                    implements.""")
            .build();

    static final ArchRule DEFINITION = noMethods().should(beAsyncAndUnproxyable());

    @ArchTest
    public static final ArchRule rule = new NoAsyncOnUnproxyableMethodRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }

    /** Used with {@code noMethods().should(...)}, so a satisfied event is reported as a violation. */
    private static ArchCondition<JavaMethod> beAsyncAndUnproxyable() {
        return new ArchCondition<>("be async and either private, static, final or declared in a final class") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Set<JavaModifier> modifiers = method.getModifiers();
                if (modifiers.contains(JavaModifier.SYNTHETIC) || !isAsync(method)) {
                    return;
                }
                String reason;
                if (modifiers.contains(JavaModifier.PRIVATE)) {
                    reason = "is private";
                } else if (modifiers.contains(JavaModifier.STATIC)) {
                    reason = "is static";
                } else if (modifiers.contains(JavaModifier.FINAL)) {
                    reason = "is final";
                } else if (method.getOwner().getModifiers().contains(JavaModifier.FINAL)) {
                    reason = "its class is final";
                } else {
                    return;
                }
                events.add(SimpleConditionEvent.satisfied(method, method.getDescription()
                        + " is async but " + reason + " in " + method.getSourceCodeLocation()));
            }
        };
    }

    private static boolean isAsync(JavaMethod method) {
        if (isAnnotatedAsync(method)) {
            return true;
        }
        Set<JavaModifier> modifiers = method.getModifiers();
        if (modifiers.contains(JavaModifier.PRIVATE) || modifiers.contains(JavaModifier.STATIC)) {
            return false;
        }
        JavaClass owner = method.getOwner();
        return Stream.concat(Stream.of(owner), owner.getAllRawSuperclasses().stream())
                .anyMatch(NoAsyncOnUnproxyableMethodRule::isAnnotatedAsync);
    }

    private static boolean isAnnotatedAsync(CanBeAnnotated element) {
        return element.isMetaAnnotatedWith(ASYNC);
    }
}
