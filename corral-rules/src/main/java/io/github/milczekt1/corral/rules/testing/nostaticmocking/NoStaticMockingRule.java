package io.github.milczekt1.corral.rules.testing.nostaticmocking;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No test class may install a Mockito static mock.
 *
 * <p>Matched through {@code MockedStatic} as well as the call, so a base class holding the handle as
 * a field and a helper taking one as a parameter are flagged where they declare it — whenever they
 * compile into test output.
 *
 * <p>Construction mocking is a separate id: the two freeze, exclude and retire independently.
 *
 * <p>Inspects <em>test</em> classes, so consumers must not set
 * {@code ImportOption.DoNotIncludeTests} — it would pass vacuously.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoStaticMockingRule implements DocumentedRule {

    /** Matched by FQN string, so nothing here needs {@code mockito-core} on the compile path. */
    static final Set<String> MOCK_HANDLE_TYPES = Set.of("org.mockito.MockedStatic");

    private static final String MOCKITO = "org.mockito.Mockito";

    /** Every overload shares the name, so matching the name alone covers all eight. */
    private static final Set<String> INSTALLER_METHODS = Set.of("mockStatic");

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.test.mockito.no-static-mocking")
            .why("""
                    mockStatic installs a stub for a class's static methods and keeps it until the \
                    returned MockedStatic is closed. The stub is thread-local, so any code under test \
                    that touches another thread — an executor, a reactive scheduler, an @Async method — \
                    sees the real static, and the test fails in a way that reads as a race. It must be \
                    closed, and if the test throws before close(), or the handle is opened in \
                    @BeforeEach with no matching teardown, the stub leaks into every later test on that \
                    thread, which then fails somewhere else entirely with nothing pointing back here. \
                    Underneath both symptoms is a design signal: a static call is a dependency with no \
                    seam, and stubbing it in the test hides that rather than fixing it, so the class \
                    under test stays unusable anywhere its statics are inconvenient — which is every \
                    other test, and every caller who wanted to supply their own clock, filesystem or \
                    id generator.""")
            .howToFix("""
                    Put a seam in. Wrap the static behind a small interface and inject it — Clock \
                    instead of Instant.now(), a FileStore port instead of Files.readAllBytes, an \
                    IdGenerator instead of UUID.randomUUID(). The test then uses an ordinary mock or a \
                    hand-written fake, works under parallel execution, and the production class gains a \
                    dependency visible in its constructor. If the static is your own code, the fix is \
                    usually to stop it being static. If the violation names a base class or a helper \
                    rather than a test, that is where the handle is declared and where the seam has to \
                    go.""")
            .howNotToFix("""
                    Do NOT move the call into a base test class or a helper that compiles into test \
                    output: the handle is matched wherever it is declared, so a base class holding one \
                    as a field and a helper taking one as a parameter are both flagged — and the \
                    leak-across-tests and thread-scoping problems get worse when the open/close pair is \
                    invisible from the test that depends on it. Do NOT delete the test because the seam \
                    is inconvenient to add, and do NOT edit the freeze store. Three dodges this \
                    predicate does NOT catch, all of them still wrong: a base class compiled into main \
                    rather than test output, which this rule never inspects; a reflective call, or a \
                    hand-rolled agent or setAccessible(true) field patch, which is the same \
                    instrumentation with no cleanup at all; and PowerMock, which is a worse answer to \
                    the same design problem — it swaps the classloader, which is why PowerMock suites \
                    break on every JDK upgrade and JaCoCo cannot measure them.""")
            .build();

    /** A local counts: the try-with-resources close() makes the handle a dependency of the method. */
    private static final DescribedPredicate<JavaClass> MOCK_HANDLE =
            describe("a Mockito static-mock handle",
                    javaClass -> MOCK_HANDLE_TYPES.contains(javaClass.getFullName()));

    /**
     * Catches the one shape the handle check misses: a result handed straight off, so no method of
     * the handle is ever called in the class that installed it.
     */
    private static final DescribedPredicate<JavaMethodCall> INSTALLS_A_MOCK =
            describe("installs a Mockito static mock",
                    call -> MOCKITO.equals(call.getTargetOwner().getFullName())
                            && INSTALLER_METHODS.contains(call.getName()));

    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.TEST_CLASSES)
            .should().dependOnClassesThat(MOCK_HANDLE)
            .orShould().callMethodWhere(INSTALLS_A_MOCK);

    @ArchTest
    public static final ArchRule rule = new NoStaticMockingRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
