package io.github.milczekt1.corral.scope;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.fixtures.source.RelocatedFixture;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SourceScopeTest {

    private static final String PACKAGE_DIR = "io/github/milczekt1/corral/fixtures/source";
    private static final String SOURCE_FILE = "RelocatedFixture.java";

    private static final ArchRule NAMING_THE_FIXTURE =
            noClasses().should().haveSimpleName("RelocatedFixture").allowEmptyShould(true);

    @TempDir
    Path module;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"target/classes", "target/test-classes", "build/classes/java/main", "build/classes/test",
            "out/production/app"})
    void aClassWhoseSourceIsInTheModulesTreeIsHandWritten(String output) throws IOException {
        plant("src/main/java/" + PACKAGE_DIR);

        assertTrue(handWritten(output));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"target/generated-sources/openapi/src/main/java",
            "target/generated-test-sources/test-annotations",
            "build/generated/sources/annotationProcessor/java/main", "build/generate-resources/main/src/main/java",
            "out/production/app/generated"})
    void aClassWhoseSourceIsUnderABuildDirectoryIsGenerated(String generatedRoot) throws IOException {
        plantOther();
        plant(generatedRoot + "/" + PACKAGE_DIR);

        assertFalse(handWritten("target/classes"));
    }

    @Test
    void theInnerClassFollowsItsSourceFile() throws IOException {
        plantOther();
        plant("target/generated-sources/annotations/" + PACKAGE_DIR);

        JavaClasses imported = importFrom("target/classes", RelocatedFixture.class, RelocatedFixture.Inner.class);

        assertFalse(SourceScope.isHandWritten(imported.get(RelocatedFixture.Inner.class), List.of()));
    }

    @Test
    void aGeneratedFileAtThePackagePathWinsOverAHandWrittenNamesakeElsewhere() throws IOException {
        plant("src/main/java/com/acme/domain");
        plant("target/generated-sources/openapi/src/main/java/" + PACKAGE_DIR);

        assertFalse(handWritten("target/classes"));
    }

    @Test
    void aHandWrittenFileInADirectoryNotMatchingItsPackageIsStillHandWritten() throws IOException {
        plant("src/main/kotlin/source");

        assertTrue(handWritten("target/classes"));
    }

    @Test
    void aClassWithNoSourceFileAnywhereIsGenerated() throws IOException {
        plantOther();

        assertFalse(handWritten("target/classes"));
    }

    @Test
    void aConfiguredDirectoryHoldsGeneratedSources() throws IOException {
        plantOther();
        plant("src/main/generated/" + PACKAGE_DIR);
        JavaClass fixture = importFrom("target/classes", RelocatedFixture.class).get(RelocatedFixture.class);

        assertFalse(SourceScope.isHandWritten(fixture, List.of("src/main/generated")));
        assertTrue(SourceScope.isHandWritten(fixture, List.of("src/main/other")));
    }

    @Test
    void nestedModulesAndDotDirectoriesAreNotTheModulesSources() throws IOException {
        plantOther();
        Files.createFile(Files.createDirectories(module.resolve("child")).resolve("pom.xml"));
        plant("child/src/main/java/" + PACKAGE_DIR);
        plant(".history/src/main/java/" + PACKAGE_DIR);

        assertFalse(handWritten("target/classes"));
    }

    @Test
    void aModuleWithoutAnySourceFileIsCheckedWhole() throws IOException {
        assertTrue(handWritten("build/classes/java/main"));
    }

    @Test
    void aClassOutsideARecognisedOutputIsChecked() throws IOException {
        plantOther();

        assertTrue(handWritten("lib/classes"));
    }

    @Test
    void aClassFromAJarIsChecked() {
        JavaClass fromJar = new ClassFileImporter().importClass(Test.class);

        assertTrue(SourceScope.isHandWritten(fromJar, List.of()), fromJar.getSource()::toString);
    }

    @Test
    void aFilteredRuleSeesOnlyHandWrittenClasses() throws IOException {
        plantOther();
        plant("target/generated-sources/openapi/" + PACKAGE_DIR);
        JavaClasses imported = importFrom("target/classes", RelocatedFixture.class);
        List<String> printed = new ArrayList<>();

        ArchRule filtered =
                SourceScope.handWrittenOnly(NAMING_THE_FIXTURE, List.of(), new AtomicBoolean(), printed::add);

        assertFalse(filtered.evaluate(imported).hasViolation());
        assertTrue(NAMING_THE_FIXTURE.evaluate(imported).hasViolation(), "the unfiltered rule must flag the fixture");
        assertEquals(1, printed.size(), printed::toString);
        assertTrue(printed.get(0).contains("1 generated class"), printed::toString);
    }

    @Test
    void aFilteredRuleStillFlagsHandWrittenClasses() throws IOException {
        plant("src/main/java/" + PACKAGE_DIR);
        List<String> printed = new ArrayList<>();

        ArchRule filtered =
                SourceScope.handWrittenOnly(NAMING_THE_FIXTURE, List.of(), new AtomicBoolean(), printed::add)
                        .as("renamed").because("kept");

        assertTrue(filtered.evaluate(importFrom("target/classes", RelocatedFixture.class)).hasViolation());
        assertEquals("renamed, because kept", filtered.getDescription());
        assertEquals(List.of(), printed, "nothing was skipped, so nothing is printed");
    }

    @Test
    void theSkippedCountIsPrintedOncePerRun() throws IOException {
        plantOther();
        JavaClasses imported = importFrom("target/classes", RelocatedFixture.class);
        AtomicBoolean printedThisRun = new AtomicBoolean();
        List<String> printed = new ArrayList<>();

        SourceScope.handWrittenOnly(NAMING_THE_FIXTURE, List.of(), printedThisRun, printed::add).evaluate(imported);
        SourceScope.handWrittenOnly(NAMING_THE_FIXTURE, List.of(), printedThisRun, printed::add).evaluate(imported);

        assertEquals(1, printed.size(), printed::toString);
    }

    @Test
    void pinsThePredicateDescription() {
        assertEquals("hand-written classes", SourceScope.HAND_WRITTEN.getDescription());
    }

    private boolean handWritten(String output) throws IOException {
        return SourceScope.isHandWritten(importFrom(output, RelocatedFixture.class).get(RelocatedFixture.class),
                List.of());
    }

    private void plant(String directory) throws IOException {
        Files.createFile(Files.createDirectories(module.resolve(directory)).resolve(SOURCE_FILE));
    }

    private void plantOther() throws IOException {
        Path other = Files.createDirectories(module.resolve("src/main/java/com/acme"));
        Files.createFile(other.resolve("Other.java"));
    }

    private JavaClasses importFrom(String output, Class<?>... fixtures) throws IOException {
        Path outputRoot = module.resolve(output);
        for (Class<?> fixture : fixtures) {
            String classFile = fixture.getName().replace('.', '/') + ".class";
            Path destination = outputRoot.resolve(classFile);
            Files.createDirectories(destination.getParent());
            try (InputStream compiled = SourceScopeTest.class.getResourceAsStream("/" + classFile)) {
                assertNotNull(compiled, classFile);
                Files.copy(compiled, destination);
            }
        }
        JavaClasses imported = new ClassFileImporter().importPath(outputRoot);
        assertEquals(fixtures.length, imported.size(), () -> "expected every copied fixture back from " + outputRoot);
        return imported;
    }
}
