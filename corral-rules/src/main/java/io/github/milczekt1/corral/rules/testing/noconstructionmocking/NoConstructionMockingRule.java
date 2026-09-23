package io.github.milczekt1.corral.rules.testing.noconstructionmocking;

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
 * No test class may install a Mockito construction mock.
 *
 * <p>Matched through {@code MockedConstruction} as well as the call, so a base class holding the
 * handle as a field and a helper taking one as a parameter are flagged where they declare it —
 * whenever they compile into test output.
 *
 * <p>Static mocking is a separate id: the two freeze, exclude and retire independently.
 *
 * <p>Inspects <em>test</em> classes, so consumers must not set
 * {@code ImportOption.DoNotIncludeTests} — it would pass vacuously.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoConstructionMockingRule implements DocumentedRule {

    /** Matched by FQN string, so nothing here needs {@code mockito-core} on the compile path. */
    static final Set<String> MOCK_HANDLE_TYPES = Set.of("org.mockito.MockedConstruction");

    private static final String MOCKITO = "org.mockito.Mockito";

    /**
     * {@code mockConstructionWithAnswer} is a separate name rather than an overload, so matching
     * {@code mockConstruction} alone would miss it.
     */
    private static final Set<String> INSTALLER_METHODS =
            Set.of("mockConstruction", "mockConstructionWithAnswer");

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.test.mockito.no-construction-mocking")
            .why("""
                    mockConstruction intercepts every `new` of a class for as long as the returned \
                    MockedConstruction is open, and hands back a mock instead. The interception is \
                    thread-local, so code under test that constructs the class on another thread — an \
                    executor, a reactive scheduler, an @Async method — gets a real instance, and the \
                    test fails in a way that reads as a race. It must be closed, and if the test throws \
                    before close(), or the handle is opened in @BeforeEach with no matching teardown, \
                    the interception leaks into every later test on that thread, which then constructs \
                    mocks it never asked for and fails somewhere else entirely. It is also the widest \
                    possible stub: every construction site is replaced, including ones inside the class \
                    under test you did not mean to touch, so the test passes on a code path that \
                    constructs nothing real and would fail the moment the collaborator is built \
                    anywhere else. Underneath is the same design signal as a static call — a hardcoded \
                    `new` is a dependency with no seam, and intercepting it hides that rather than \
                    fixing it.""")
            .howToFix("""
                    Pass the collaborator in rather than constructing it. A constructor parameter, a \
                    factory injected as an interface, or a supplier the test can substitute all turn \
                    the hidden `new` into a dependency the compiler shows you, and the test then uses \
                    an ordinary mock or a hand-written fake that works under parallel execution. Where \
                    the collaborator is genuinely cheap and has no I/O, the better fix is usually to \
                    stop mocking it and assert on the real thing. If the violation names a base class \
                    or a helper rather than a test, that is where the handle is declared and where the \
                    seam has to go.""")
            .howNotToFix("""
                    Do NOT move the call into a base test class or a helper that compiles into test \
                    output: the handle is matched wherever it is declared, so a base class holding one \
                    as a field and a helper taking one as a parameter are both flagged — and the \
                    leak-across-tests and thread-scoping problems get worse when the open/close pair is \
                    invisible from the test that depends on it. Do NOT swap mockConstruction for \
                    mockConstructionWithAnswer; it is the same interception and is matched too. Do NOT \
                    delete the test because the seam is inconvenient to add, and do NOT edit the freeze \
                    store. Three dodges this predicate does NOT catch, all of them still wrong: a base \
                    class compiled into main rather than test output, which this rule never inspects; a \
                    reflective call, or a hand-rolled agent, which is the same instrumentation with no \
                    cleanup at all; and PowerMock's constructor stubbing, which is a worse answer to \
                    the same design problem — it swaps the classloader, which is why PowerMock suites \
                    break on every JDK upgrade and JaCoCo cannot measure them.""")
            .build();

    /** A local counts: the try-with-resources close() makes the handle a dependency of the method. */
    private static final DescribedPredicate<JavaClass> MOCK_HANDLE =
            describe("a Mockito construction-mock handle",
                    javaClass -> MOCK_HANDLE_TYPES.contains(javaClass.getFullName()));

    /**
     * Catches the one shape the handle check misses: a result handed straight off, so no method of
     * the handle is ever called in the class that installed it.
     */
    private static final DescribedPredicate<JavaMethodCall> INSTALLS_A_MOCK =
            describe("installs a Mockito construction mock",
                    call -> MOCKITO.equals(call.getTargetOwner().getFullName())
                            && INSTALLER_METHODS.contains(call.getName()));

    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.TEST_CLASSES)
            .should().dependOnClassesThat(MOCK_HANDLE)
            .orShould().callMethodWhere(INSTALLS_A_MOCK);

    @ArchTest
    public static final ArchRule rule = new NoConstructionMockingRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
