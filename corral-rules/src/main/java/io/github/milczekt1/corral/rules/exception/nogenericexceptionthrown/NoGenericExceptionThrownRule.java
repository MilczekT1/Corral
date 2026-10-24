package io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static java.util.stream.Collectors.toUnmodifiableSet;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.domain.AccessTarget.ConstructorReferenceTarget;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import java.util.Set;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No production class may call or reference a constructor of exactly {@code Throwable},
 * {@code Error}, {@code Exception} or {@code RuntimeException}.
 *
 * <p>A call from a constructor of a named direct subclass to its superclass's constructor is exempt.
 * ArchUnit cannot tell {@code super(...)} from any other such call there, so an instance of the
 * superclass's own type built in that constructor — {@code super(msg, new RuntimeException("inner"))}
 * in a {@code RuntimeException} subclass, an instance field initialiser, or a lambda there — is
 * missed; one of the other three types, and a constructor reference, are still flagged. An instance
 * field initialiser is reported once per constructor that runs it, under that constructor's
 * signature. An anonymous subclass is reported on its own constructor ({@code OrderService$1.<init>}), not on the
 * method that throws it. A rethrow of a caught throwable is never matched: bytecode has no typed
 * {@code throw}, so construction is the only thing to see.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoGenericExceptionThrownRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.exception.no-generic-exception-thrown")
            .why("""
                    throw new RuntimeException("order not found") is a failure with no type. Nothing downstream can \
                    react to it except by matching the message text. A caller cannot catch it selectively: a \
                    catch (RuntimeException e) also catches every NullPointerException and every library's \
                    IllegalStateException. Retry policies, @ControllerAdvice handlers and circuit breakers dispatch \
                    on type. With one untyped exception, a retryable timeout and a permanent "not found" take the \
                    same path: either the timeout is not retried, or the "not found" is retried and then answered \
                    with a 500 instead of a 404.""")
            .howToFix("""
                    Throw a named exception for the failure — OrderNotFoundException, PaymentDeclinedException. \
                    Extend RuntimeException, or Exception when the caller is expected to handle it. When wrapping a \
                    caught checked exception, pass it as the cause: UncheckedIOException for an IOException, \
                    otherwise a named type of your own. One type per reason, not one per call site. For a checked \
                    exception that cannot happen — UnsupportedEncodingException for "UTF-8", URISyntaxException on \
                    a constant — call the overload that does not throw (URLEncoder.encode(s, StandardCharsets.UTF_8) \
                    from Java 10, URI.create). Where none exists, as for NoSuchAlgorithmException on "SHA-256", \
                    throw IllegalStateException with the cause. An Error thrown for a state that cannot happen \
                    becomes AssertionError. A Throwable built only to record a stack trace for a log line becomes a \
                    named subclass (class CallerTrace extends Exception {}). A violation in generated code is fixed \
                    in the generator's template or configuration, never in the generated file, which the next build \
                    overwrites.""")
            .howNotToFix("""
                    Do NOT swap in another JDK type that does not name the failure — IllegalStateException, \
                    IllegalArgumentException, UnsupportedOperationException, CompletionException, or \
                    NoSuchElementException from a bare orElseThrow(). This rule does not catch them, but used for a \
                    failure a caller may need to handle, they recreate the same problem. Do NOT add one catch-all \
                    type (AppException, ServiceException) and throw it for every failure: this rule does not catch \
                    it, and it is the same untyped failure under a new name. Do NOT move the new \
                    RuntimeException(...) into a helper such as Exceptions.fail(msg): the helper is flagged but its \
                    callers are not, so routing new code through an already-frozen helper passes the build and is \
                    still wrong. Do NOT call a library helper that builds one for you, such as Guava's \
                    Throwables.propagate(e): the RuntimeException is constructed inside the library, so this rule \
                    does not catch it. Do NOT build the generic exception inside the constructor of a direct subclass \
                    of it, as super(msg, new RuntimeException(...)) in a RuntimeException subclass: that call cannot be told from super(...), so this rule \
                    does not catch it. Do NOT return null or an empty Optional instead of throwing: this rule does \
                    not catch it, and it turns a loud failure into a silent one. Do NOT replace the wrapper with \
                    Lombok's @SneakyThrows or a sneaky-throw helper: no constructor is called, so this rule does not \
                    catch it, and it throws a checked exception the method does not declare, which a caller then \
                    cannot catch by type. Do NOT try to move the class out of scope by naming it like a test: scope \
                    is decided by output location and declared test methods; a stub in a test-support module's \
                    src/main, or in Gradle testFixtures or integrationTest output, is production by that measure and \
                    is flagged. Do NOT add a JUnit test method to a production class: a class declaring one counts as \
                    a test class, so this is not caught.""")
            .build();

    private static final Set<String> GENERIC_THROWABLES =
            Stream.of(Throwable.class, Error.class, Exception.class, RuntimeException.class)
                    .map(Class::getName)
                    .collect(toUnmodifiableSet());

    private static final DescribedPredicate<JavaAccess<?>> GENERIC_THROWABLE_CONSTRUCTION = describe(
            "target is a constructor of Throwable, Error, Exception or RuntimeException, other than a direct "
                    + "subclass's constructor calling its superclass's",
            access -> isConstructor(access.getTarget())
                    && GENERIC_THROWABLES.contains(access.getTargetOwner().getName())
                    && !isSuperclassConstructorCall(access));

    /** Do not switch to {@code callConstructorWhere}: it misses a {@code RuntimeException::new} reference. */
    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.PRODUCTION_CLASSES)
            .should().accessTargetWhere(GENERIC_THROWABLE_CONSTRUCTION);

    @ArchTest
    public static final ArchRule rule = new NoGenericExceptionThrownRule().guard();

    private static boolean isConstructor(AccessTarget target) {
        return target instanceof ConstructorCallTarget || target instanceof ConstructorReferenceTarget;
    }

    private static boolean isSuperclassConstructorCall(JavaAccess<?> access) {
        JavaClass caller = access.getOriginOwner();
        return access instanceof JavaConstructorCall
                && access.getOrigin() instanceof JavaConstructor
                && !caller.isAnonymousClass()
                && caller.getRawSuperclass().filter(access.getTargetOwner()::equals).isPresent();
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
