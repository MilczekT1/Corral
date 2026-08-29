package io.github.milczekt1.corral.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.fixtures.scope.NoTestMethodsFixture;
import io.github.milczekt1.corral.fixtures.scope.ParameterizedTestMethodFixture;
import io.github.milczekt1.corral.fixtures.scope.PlainTestMethodFixture;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TestScopeTest {

    /** Gradle's <em>main</em> output — matched by none of the test-location patterns. */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

    /** This build is Maven, so only {@code target/**} occurs for real; the rest are copied trees. */
    @ParameterizedTest(name = "{0} is test output")
    @ValueSource(strings = {
            "build/classes/java/test",  // Gradle, with the language directory
            "build/classes/test",       // Gradle, without it — the optional group in the pattern
            "out/test/corral-sdk"})     // IntelliJ
    void recognisesTheBuildLayoutsThisBuildCannotProduce(String layout, @TempDir Path outputRoot)
            throws IOException {
        JavaClasses imported = importFrom(outputRoot, layout, NoTestMethodsFixture.class);

        JavaClass fixture = imported.get(NoTestMethodsFixture.class);
        assertTrue(TestScope.TEST_CLASSES.test(fixture),
                () -> "a class compiled into " + layout + " must be in test scope on location alone,"
                        + " whatever it declares; source was " + sourceUriOf(fixture));
        assertFalse(TestScope.PRODUCTION_CLASSES.test(fixture), sourceUriOf(fixture));
    }

    /** Neither class declares a test method, so location is the only thing separating them. */
    @Test
    void separatesThisBuildsOwnMainOutputFromItsTestOutput() {
        JavaClasses thisBuild = new ClassFileImporter()
                .importClasses(RuleDoc.class, NoTestMethodsFixture.class);
        assertFalse(thisBuild.isEmpty(), "nothing imported — every assertion below would be vacuous");

        JavaClass mainClass = thisBuild.get(RuleDoc.class);
        JavaClass testClass = thisBuild.get(NoTestMethodsFixture.class);

        assertTrue(TestScope.PRODUCTION_CLASSES.test(mainClass), sourceUriOf(mainClass));
        assertFalse(TestScope.TEST_CLASSES.test(mainClass), sourceUriOf(mainClass));
        assertTrue(TestScope.TEST_CLASSES.test(testClass), sourceUriOf(testClass));
        assertFalse(TestScope.PRODUCTION_CLASSES.test(testClass), sourceUriOf(testClass));
    }

    /**
     * Imported from a production-looking directory, so only what a class declares can decide.
     * {@code ParameterizedTestMethodFixture} is reached only through {@code @TestTemplate}.
     */
    @Test
    void findsTestClassesByDeclaredTestMethodsWhereTheLocationSaysNothing(@TempDir Path outputRoot)
            throws IOException {
        JavaClasses imported = importFrom(outputRoot, PRODUCTION_LAYOUT,
                PlainTestMethodFixture.class, ParameterizedTestMethodFixture.class,
                NoTestMethodsFixture.class);

        JavaClass plainTest = imported.get(PlainTestMethodFixture.class);
        JavaClass parameterizedTest = imported.get(ParameterizedTestMethodFixture.class);
        JavaClass noTests = imported.get(NoTestMethodsFixture.class);

        assertTrue(TestScope.TEST_CLASSES.test(plainTest), sourceUriOf(plainTest));
        assertTrue(TestScope.TEST_CLASSES.test(parameterizedTest), sourceUriOf(parameterizedTest));
        assertFalse(TestScope.TEST_CLASSES.test(noTests), sourceUriOf(noTests));
        assertTrue(TestScope.PRODUCTION_CLASSES.test(noTests), sourceUriOf(noTests));
    }

    /**
     * Producing a stub needs ArchUnit's classpath resolution off — process-wide config in a JVM
     * Surefire reuses, so it is restored in a {@code finally}.
     */
    @Test
    void treatsStubClassesWithNoSourceAsProduction() {
        ArchConfiguration configuration = ArchConfiguration.get();
        boolean resolvedFromClasspath = configuration.resolveMissingDependenciesFromClassPath();
        configuration.setResolveMissingDependenciesFromClassPath(false);
        try {
            JavaClass stub = new ClassFileImporter()
                    .importClasses(NoTestMethodsFixture.class)
                    .get(NoTestMethodsFixture.class)
                    .getRawSuperclass()
                    .orElseThrow();

            assertTrue(stub.getSource().isEmpty(),
                    () -> stub.getName() + " was expected to be an unimported stub with no source");
            assertTrue(TestScope.PRODUCTION_CLASSES.test(stub), stub.getName());
            assertFalse(TestScope.TEST_CLASSES.test(stub), stub.getName());
        } finally {
            configuration.setResolveMissingDependenciesFromClassPath(resolvedFromClasspath);
        }
    }

    /**
     * These descriptions render into every rule built on them, and a rule description is a
     * freeze-store key.
     */
    @Test
    void pinsThePredicateDescriptions() {
        assertEquals("test classes", TestScope.TEST_CLASSES.getDescription());
        assertEquals("production classes", TestScope.PRODUCTION_CLASSES.getDescription());
    }

    @Test
    void productionScopeIsTheExactComplementOfTestScope(@TempDir Path outputRoot) throws IOException {
        JavaClasses everyShape = importFrom(outputRoot, PRODUCTION_LAYOUT,
                PlainTestMethodFixture.class, ParameterizedTestMethodFixture.class,
                NoTestMethodsFixture.class);

        for (JavaClass clazz : everyShape) {
            assertEquals(!TestScope.TEST_CLASSES.test(clazz), TestScope.PRODUCTION_CLASSES.test(clazz),
                    () -> clazz.getName() + " must be in exactly one of the two scopes");
        }
    }

    /**
     * Copies the compiled class files of {@code fixtures} into {@code root/layout}, keeping package
     * directories, and imports from there.
     */
    private static JavaClasses importFrom(Path root, String layout, Class<?>... fixtures)
            throws IOException {
        Path outputRoot = root.resolve(layout);
        for (Class<?> fixture : fixtures) {
            String classFile = fixture.getName().replace('.', '/') + ".class";
            Path destination = outputRoot.resolve(classFile);
            Files.createDirectories(destination.getParent());
            try (InputStream compiled = TestScopeTest.class.getResourceAsStream("/" + classFile)) {
                assertNotNull(compiled, () -> "no compiled class file on the classpath for " + classFile);
                Files.copy(compiled, destination);
            }
        }

        JavaClasses imported = new ClassFileImporter().importPath(outputRoot);
        assertEquals(fixtures.length, imported.size(),
                () -> "expected every copied fixture back from " + outputRoot
                        + " — an import that finds nothing makes every assertion on it vacuous");
        return imported;
    }

    private static String sourceUriOf(JavaClass clazz) {
        return clazz.getName() + " imported from "
                + clazz.getSource().map(source -> source.getUri().toString()).orElse("<no source>");
    }
}
