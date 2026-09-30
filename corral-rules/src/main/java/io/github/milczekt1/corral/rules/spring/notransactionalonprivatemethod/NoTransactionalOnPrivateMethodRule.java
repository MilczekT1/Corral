package io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No {@code private} method may be annotated or meta-annotated {@code @Transactional} — Spring's,
 * {@code jakarta}'s or {@code javax}'s.
 *
 * <p>Matched by annotation name, so a consumer without these libraries on the classpath still
 * compiles and runs the rule. Only the method's own annotations count: a class-level
 * {@code @Transactional} never applies to a private method.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoTransactionalOnPrivateMethodRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.no-transactional-on-private-method")
            .why("""
                    Spring's declarative transactions live in a proxy around the bean. A CGLIB proxy \
                    is a runtime subclass that overrides a method, opens the transaction, calls super, \
                    then commits or rolls back; a JDK proxy only sees interface methods. A private \
                    method can be neither overridden nor declared on an interface, and a call to it \
                    from inside the bean never leaves the target object, so no interceptor is ever on \
                    the call path. Nothing warns you: the context starts, the annotation is right there \
                    in the source, and every repository call inside the method auto-commits on its \
                    own. The rollback the annotation was written for does not exist. From Spring 6.0 \
                    class-based proxies also honour protected and package-private methods; private is \
                    the modifier that stays inert on every Spring version and every proxy type.""")
            .howToFix("""
                    Decide where the transaction boundary belongs. Usually the private method is a \
                    helper and the boundary belongs on the public entry point that calls it: move the \
                    annotation there. If the private method really is the unit of work, extract it into \
                    its own bean with a non-private method, inject that bean, and call it through the \
                    injected reference so the call goes through the proxy.""")
            .howNotToFix("""
                    Do NOT just widen the modifier to package-private or public and keep calling the \
                    method from the same class: the call still never passes through the proxy, so there \
                    is still no transaction. This check cannot see that — it reads the modifier, not \
                    the call site — so it would go quiet without anything being fixed. Do NOT widen it \
                    and keep the annotation without checking what the method now spans: a no-op \
                    annotation becomes a real transaction that may hold a connection across an HTTP \
                    call. Do NOT switch to AspectJ weaving (AdviceMode.ASPECTJ) to make the premise go \
                    away. Do NOT delete the annotation "because it did nothing anyway" without asking \
                    whether the code needed a transaction; usually it did. Not caught: protected and \
                    package-private @Transactional methods, which are inert too on Spring 5.""")
            .build();

    static final ArchRule DEFINITION = noMethods()
            .that().areMetaAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .or().areMetaAnnotatedWith("jakarta.transaction.Transactional")
            .or().areMetaAnnotatedWith("javax.transaction.Transactional")
            .should().bePrivate();

    @ArchTest
    public static final ArchRule rule = new NoTransactionalOnPrivateMethodRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
