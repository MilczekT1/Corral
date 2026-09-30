package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic;

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
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No transactional method may be {@code final}, {@code static}, or declared in a {@code final} class.
 *
 * <p>A method is transactional when it is meta-annotated {@code @Transactional} — Spring's,
 * {@code jakarta}'s or {@code javax}'s — or, unless it is static, when its class or any superclass
 * is. Matched by annotation name, so a consumer without these libraries still runs the rule.
 * Private and synthetic methods, bridge methods included, are never reported.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoTransactionalOnFinalOrStaticRule implements DocumentedRule {

    private static final List<String> TRANSACTIONAL = List.of(
            "org.springframework.transaction.annotation.Transactional",
            "jakarta.transaction.Transactional",
            "javax.transaction.Transactional");

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.no-transactional-on-final-or-static")
            .why("""
                    Spring's declarative transactions live in a proxy around the bean, and Spring Boot \
                    builds that proxy with CGLIB by default: a runtime subclass that overrides each \
                    advised method, opens the transaction, calls super, then commits or rolls back. \
                    Three shapes cannot be overridden, and each fails in its own way. A final class \
                    cannot be subclassed at all, so startup fails with "Cannot subclass final class" — \
                    loud, but only once that bean is actually wired, which may be one profile or one \
                    integration test away. A final method on a proxyable class is worse: the proxy is \
                    created, every other method is advised, and this one runs directly on the target. \
                    A static method is the same, since it belongs to the class and no proxy is ever on \
                    its call path. In both cases the annotation is in the source, no transaction ever \
                    begins, and every write inside auto-commits on its own; the rollback the annotation \
                    was written for does not exist. A class-level @Transactional reaches every instance \
                    method, so marking one of them final opens the same hole with no annotation next to \
                    it to prompt a second look. Spring logs the skipped method at DEBUG at most.""")
            .howToFix("""
                    For a final method or a final class, remove the final modifier. If the class is \
                    final for a reason — value semantics, a deliberate no-subclass contract — it is not \
                    the place for a transaction boundary: move the transactional method into a service \
                    bean that may be proxied and keep the final class as a collaborator it calls. If a \
                    class-level @Transactional is what makes a final method transactional, decide \
                    whether that method needs a transaction; if it does not, move it out of the \
                    annotated class and it may stay final. For a static method, make it an instance \
                    method of a bean and call it through the injected bean, so the call passes through \
                    the proxy; if it needs no transaction, remove the annotation.""")
            .howNotToFix("""
                    Do NOT set spring.aop.proxy-target-class=false to get JDK dynamic proxies: that \
                    changes proxying for the whole application, breaks every injection point typed by \
                    the concrete class, and advises only methods declared on an interface. Do NOT \
                    switch to AspectJ weaving (AdviceMode.ASPECTJ) to make the premise go away; that is \
                    a new runtime concern adopted to silence one check. Do NOT delete @Transactional \
                    "because it was not working anyway" without deciding whether the operation needs \
                    atomicity — it usually did. Do NOT make the method private to get it past this \
                    check: the proxy cannot advise a private method either, so the transaction is still \
                    missing. Do NOT turn a static method into an instance method and keep calling it \
                    from inside its own class: that call never leaves the target object, so there is \
                    still no transaction, and this check reads modifiers, not call sites, so it would \
                    go quiet without anything being fixed. Not caught: a @Transactional declared only \
                    on an interface the class implements, and a static method in an annotated class \
                    that carries no annotation of its own.""")
            .build();

    static final ArchRule DEFINITION = noMethods().should(beTransactionalAndUnproxyable());

    @ArchTest
    public static final ArchRule rule = new NoTransactionalOnFinalOrStaticRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }

    /** Used with {@code noMethods().should(...)}, so a satisfied event is reported as a violation. */
    private static ArchCondition<JavaMethod> beTransactionalAndUnproxyable() {
        return new ArchCondition<>("be transactional and either final, static or declared in a final class") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Set<JavaModifier> modifiers = method.getModifiers();
                if (modifiers.contains(JavaModifier.PRIVATE)
                        || modifiers.contains(JavaModifier.SYNTHETIC)
                        || !isTransactional(method)) {
                    return;
                }
                JavaClass owner = method.getOwner();
                String reason;
                if (modifiers.contains(JavaModifier.STATIC)) {
                    reason = "is static";
                } else if (modifiers.contains(JavaModifier.FINAL)) {
                    reason = "is final";
                } else if (owner.getModifiers().contains(JavaModifier.FINAL)) {
                    reason = "its class is final";
                } else {
                    return;
                }
                events.add(SimpleConditionEvent.satisfied(method, method.getDescription()
                        + " is transactional but " + reason + " in " + method.getSourceCodeLocation()));
            }
        };
    }

    private static boolean isTransactional(JavaMethod method) {
        if (isAnnotatedTransactional(method)) {
            return true;
        }
        if (method.getModifiers().contains(JavaModifier.STATIC)) {
            return false;
        }
        JavaClass owner = method.getOwner();
        return Stream.concat(Stream.of(owner), owner.getAllRawSuperclasses().stream())
                .anyMatch(NoTransactionalOnFinalOrStaticRule::isAnnotatedTransactional);
    }

    private static boolean isAnnotatedTransactional(CanBeAnnotated element) {
        return TRANSACTIONAL.stream().anyMatch(element::isMetaAnnotatedWith);
    }
}
