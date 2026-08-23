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

    /**
     * A layout none of the three patterns match — Gradle's <em>main</em> output. Importing the
     * fixtures from here switches the location half off, so what remains is the structure half on
     * its own, and the same import doubles as the control proving the temporary directory itself
     * does not accidentally look like test output.
     */
    private static final String PRODUCTION_LAYOUT = "build/classes/java/main";

    /**
     * This build is Maven, so it can only ever produce {@code target/test-classes} and
     * {@code target/classes} for real. The Gradle and IntelliJ layouts are exercised by copying
     * genuinely compiled class files into a directory tree shaped like the layout under test and
     * importing from there: the {@code JavaClass} is real, its source URI is real, and the only
     * thing synthesised is the directory name — which is exactly the input under test.
     */
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

    /**
     * The location half against the layout this build really does produce. Both classes are read out
     * of this very Maven build: {@code RuleDoc} compiles to {@code target/classes} and the fixture
     * to {@code target/test-classes}, and neither declares a test method — so location is the only
     * thing that can be telling them apart.
     */
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
     * The structure half with the location half switched off: all three fixtures are imported from a
     * production-looking directory, so only what they declare can decide.
     *
     * <p>{@code ParameterizedTestMethodFixture} is the one that matters most — it holds no
     * {@code @Test} anywhere and is reached only through {@code @TestTemplate}, so a direct
     * annotation check would drop it. {@code NoTestMethodsFixture} is the other direction: without
     * it, a predicate that simply said "yes" would pass every other assertion here.
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
     * A stub class — referenced by imported code but never imported itself — has no source, and must
     * land on the production side rather than blowing up on the absent {@link java.util.Optional}.
     *
     * <p>Producing one takes turning off ArchUnit's classpath resolution, which is process-wide
     * configuration in a JVM Surefire reuses across every test class, so it is restored in a
     * {@code finally} whatever happens. The emptiness of the source is asserted rather than assumed:
     * if ArchUnit ever starts handing stubs a source, this test must go red instead of quietly
     * asserting nothing.
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
     * Both predicates' descriptions end up inside the rendered description of every rule built on
     * them, and a rule's description is its freeze-store key. Pinning them here makes a reword show
     * up as a failing test rather than as a consumer's build failing on code they did not touch.
     */
    @Test
    void pinsThePredicateDescriptions() {
        assertEquals("test classes", TestScope.TEST_CLASSES.getDescription());
        assertEquals("production classes", TestScope.PRODUCTION_CLASSES.getDescription());
    }

    /**
     * The two scopes must partition, not merely differ: a class in both would be reported by a
     * test-scoped and a production-scoped rule at once, and a class in neither would be silently
     * exempt from both.
     */
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
     * Copies the already-compiled class files of {@code fixtures} into {@code root/layout}, keeping
     * their package directories, and imports them from there. Nothing is generated: the bytes are
     * the ones {@code javac} produced for this build, so the only difference from a normal import is
     * the directory the source URI points into.
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
