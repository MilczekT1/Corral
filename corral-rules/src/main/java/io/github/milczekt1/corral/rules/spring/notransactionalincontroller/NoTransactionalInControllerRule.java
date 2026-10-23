package io.github.milczekt1.corral.rules.spring.notransactionalincontroller;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No production class meta-annotated {@code @Controller} — {@code @RestController} included — may
 * carry {@code @Transactional} (Spring's, {@code jakarta}'s or {@code javax}'s), on itself, on a
 * superclass or interface it inherits from, or on any method it declares or inherits.
 *
 * <p>Stands alone, in no group: wire it with {@code ArchTests.in(NoTransactionalInControllerRule.class)}.
 * Matched by annotation name, so the class loads without Spring on the classpath. It needs ArchUnit's
 * default {@code resolveMissingDependenciesFromClassPath=true}: with it off, {@code @RestController}
 * classes are not recognised. One violation per annotated element, each naming the controller.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoTransactionalInControllerRule implements DocumentedRule {

    private static final List<String> TRANSACTIONAL = List.of(
            "org.springframework.transaction.annotation.Transactional",
            "jakarta.transaction.Transactional",
            "javax.transaction.Transactional");

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.no-transactional-in-controller")
            .why("""
                    A @Transactional controller method holds one database connection for the whole \
                    handler body: calls to several services, calls to remote HTTP APIs, and any other \
                    slow work in the handler. The pool is sized for database work, so a slow remote call \
                    now starves it. Operations from unrelated services commit or roll back together only \
                    because one request called them, not because anyone decided they form one unit of \
                    work. And a handler that needs a transaction is doing business logic that belongs in \
                    the service layer.""")
            .howToFix("""
                    Move the annotation down. Put @Transactional on the service method the controller \
                    calls. If the controller calls several services that must commit together, that is \
                    one use case, not three calls: give it one service method and annotate that. Move \
                    the whole unit of work, not just the annotation. If the handler loads an entity and \
                    changes it without calling save, only the transaction's flush writes that change: \
                    move both the load and the change into the service method, or the update is silently \
                    lost. If the controller calls a repository directly, the service method is new: \
                    create it. The controller now runs outside any transaction, so have the service \
                    return everything the response needs: a DTO, or entities with the associations the \
                    response reads already fetched (join fetch, entity graph). Keep remote calls out of \
                    the transactional service method. readOnly = true is flagged the same way: it still \
                    holds a connection for the whole handler body. propagation = SUPPORTS, NOT_SUPPORTED \
                    or NEVER is flagged too: it opens no transaction, but it keeps one EntityManager open \
                    across the handler, so lazy loads there may depend on it. Delete it and fetch what \
                    the response needs in the service, as above.""")
            .howNotToFix("""
                    Do NOT move the annotation to another method of the same controller: a call from \
                    inside the class bypasses the proxy, so it does nothing, and this check still flags \
                    it. Do NOT move it to a base class or an interface the controller implements: this \
                    check flags inherited annotations too. Do NOT replace it with a TransactionTemplate \
                    or PlatformTransactionManager in the controller. Not caught: this check reads \
                    annotations, not dependencies. Do NOT open the transaction around the request \
                    instead: in a servlet Filter, a HandlerInterceptor, or an @Around aspect on \
                    controller methods. Not caught: this check reads only controller classes. Do NOT \
                    swap it for EJB's @TransactionAttribute: Spring honours that too when the EJB API is \
                    on the classpath. Not caught: this check matches only Spring's, jakarta's and \
                    javax's @Transactional. Do NOT \
                    rely on Open Session In View (spring.jpa.open-in-view, on by default in Spring Boot) \
                    or hibernate.enable_lazy_load_no_trans to get past a LazyInitializationException \
                    after the move. Not caught: this check cannot see configuration.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.PRODUCTION_CLASSES)
            .and().areMetaAnnotatedWith("org.springframework.stereotype.Controller")
            .should(declareOrInheritTransactional());

    @ArchTest
    public static final ArchRule rule = new NoTransactionalInControllerRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }

    /**
     * Used with {@code noClasses().should(...)}, so a satisfied event is reported as a violation.
     * Do not put the matched annotation's name in the report text: a {@code javax} to
     * {@code jakarta} migration would then reword frozen lines.
     */
    private static ArchCondition<JavaClass> declareOrInheritTransactional() {
        return new ArchCondition<>("declare or inherit @Transactional") {
            @Override
            public void check(JavaClass controller, ConditionEvents events) {
                String name = controller.getDescription();
                if (isTransactional(controller)) {
                    events.add(SimpleConditionEvent.satisfied(controller, name + " is @Transactional"));
                }
                Stream.concat(controller.getAllRawSuperclasses().stream(), controller.getAllRawInterfaces().stream())
                        .filter(NoTransactionalInControllerRule::isTransactional)
                        .forEach(type -> events.add(SimpleConditionEvent.satisfied(controller,
                                name + " inherits @Transactional from <" + type.getName() + ">")));
                // javac copies a method's annotations onto its bridge methods.
                controller.getAllMethods().stream()
                        .filter(method -> !method.getModifiers().contains(JavaModifier.SYNTHETIC))
                        .filter(NoTransactionalInControllerRule::isTransactional)
                        .forEach(method -> events.add(SimpleConditionEvent.satisfied(controller,
                                name + " has @Transactional method <" + method.getFullName() + "> in "
                                        + method.getSourceCodeLocation())));
            }
        };
    }

    private static boolean isTransactional(CanBeAnnotated element) {
        return TRANSACTIONAL.stream().anyMatch(element::isMetaAnnotatedWith);
    }
}
