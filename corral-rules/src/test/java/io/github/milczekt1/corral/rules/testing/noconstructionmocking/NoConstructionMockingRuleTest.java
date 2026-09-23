package io.github.milczekt1.corral.rules.testing.noconstructionmocking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import io.github.milczekt1.corral.rules.testing.noconstructionmocking.fixtures.ConstructionHandleHolder;
import io.github.milczekt1.corral.rules.testing.noconstructionmocking.fixtures.ConstructionHandleReceiver;
import io.github.milczekt1.corral.rules.testing.noconstructionmocking.fixtures.ConstructionHandoffCaller;
import io.github.milczekt1.corral.rules.testing.noconstructionmocking.fixtures.ConstructionMockingCaller;
import io.github.milczekt1.corral.rules.testing.noconstructionmocking.fixtures.PlainMockitoUser;
import io.github.milczekt1.corral.rules.testing.noconstructionmocking.fixtures.WithAnswerHandoffCaller;
import io.github.milczekt1.corral.scope.TestScope;
import io.github.milczekt1.corral.store.EmptyOmittingViolationStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

/**
 * The examples live in {@code fixtures/}, which Surefire and Sonar both exclude: they exist to be
 * flagged, and a linter told to tidy any of them would delete the violation under test.
 */
class NoConstructionMockingRuleTest {

    private static final String ID = "corral.test.mockito.no-construction-mocking";

    /** Resolved against the JVM working directory, which under Surefire is the module. */
    private static final String STORE_PATH = "src/test/resources/archunit/frozen";

    /**
     * {@code Mockito} itself installs construction mocks and is loaded from a jar, so it is
     * production-scoped: it pins the scope clause.
     */
    private static final JavaClasses EXAMPLES = new ClassFileImporter().importClasses(
            ConstructionMockingCaller.class, ConstructionHandleHolder.class,
            ConstructionHandleReceiver.class, ConstructionHandoffCaller.class,
            WithAnswerHandoffCaller.class, PlainMockitoUser.class, Mockito.class);

    /** The raw {@code DEFINITION}: the published field is frozen, so it would seed and pass. */
    private static String report() {
        return String.join("\n", NoConstructionMockingRule.DEFINITION
                .allowEmptyShould(true).evaluate(EXAMPLES).getFailureReport().getDetails());
    }

    static Stream<Arguments> flags() {
        return Stream.of(
                arguments("a mockConstruction call", "ConstructionMockingCaller"),
                arguments("the handle held as a field", "ConstructionHandleHolder"),
                arguments("the handle taken as a parameter", "ConstructionHandleReceiver"),
                arguments("a handle handed straight off", "ConstructionHandoffCaller"),
                arguments("a mockConstructionWithAnswer handle handed straight off",
                        "WithAnswerHandoffCaller"));
    }

    @ParameterizedTest(name = "flags {0}")
    @MethodSource
    void flags(String description, String offender) {
        String report = report();

        assertTrue(report.contains(offender), () -> "must flag " + description + ": " + report);
    }

    @Test
    void ignoresOrdinaryMockito() {
        String report = report();

        assertFalse(report.contains("PlainMockitoUser"), report);
    }

    /** The premise is a class both clauses would match, excluded only by scope. */
    @Test
    void scopesItselfToTestClasses() {
        JavaClass mockito = EXAMPLES.get(Mockito.class);

        assertFalse(TestScope.TEST_CLASSES.test(mockito),
                "premise: Mockito is loaded from a jar, so it has no test-output source");
        assertTrue(mockito.getDirectDependenciesFromSelf().stream()
                        .anyMatch(d -> NoConstructionMockingRule.MOCK_HANDLE_TYPES
                                .contains(d.getTargetClass().getFullName())),
                "premise: Mockito must still depend on the handle type, or this pins nothing");

        assertFalse(report().contains("Method <org.mockito.Mockito."),
                () -> "Mockito appears only as a call target, never as an offender: " + report());

        String description = NoConstructionMockingRule.DEFINITION.getDescription();
        assertTrue(description.contains("test classes"), description);
    }

    /**
     * {@link FreezingArchRule#persistIn}, not {@code freeze.store}: a frozen rule captures its store
     * when constructed, which is class initialisation, so naming one here races class loading. Only
     * the path goes on the process-wide {@link ArchConfiguration}.
     *
     * <p>Reseed with {@code -Darchunit.freeze.store.default.allowStoreCreation=true}, then commit.
     */
    @Test
    void freezesWhatItFindsIntoTheCommittedStore() throws IOException {
        ArchConfiguration.get().setProperty("freeze.store.default.path", STORE_PATH);
        try {
            ArchRule frozen = assertInstanceOf(FreezingArchRule.class, NoConstructionMockingRule.rule,
                    "the published field must be frozen — an unfrozen rule fails on adoption")
                    .persistIn(new EmptyOmittingViolationStore());

            frozen.check(EXAMPLES);

            String index = Files.readString(Path.of(STORE_PATH, "stored.rules"));
            assertTrue(index.contains(ID + "=" + ID),
                    "the index must file this rule's debt under its id: " + index);

            String debt = Files.readString(Path.of(STORE_PATH, ID));
            assertTrue(debt.contains("ConstructionMockingCaller"), debt);
            assertTrue(debt.contains("WithAnswerHandoffCaller"), debt);
        } finally {
            ArchConfiguration.get().reset();
        }
    }
}
