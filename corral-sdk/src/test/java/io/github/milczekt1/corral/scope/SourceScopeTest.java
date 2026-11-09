package io.github.milczekt1.corral.scope;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.Source;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.fixtures.source.RelocatedFixture;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.tools.ToolProvider;
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
    void aClassWhoseSourceIsInTheModulesTreeIsMaintained(String output) throws IOException {
        plant("src/main/java/" + PACKAGE_DIR);

        assertTrue(fixtureIsMaintained(output));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"target/generated-sources/openapi/src/main/java",
            "target/generated-test-sources/test-annotations",
            "build/generated/sources/annotationProcessor/java/main", "build/generate-resources/main/src/main/java",
            "out/production/app/generated"})
    void aClassWhoseSourceIsUnderABuildDirectoryIsGenerated(String generatedRoot) throws IOException {
        plantOther();
        plant(generatedRoot + "/" + PACKAGE_DIR);

        assertFalse(fixtureIsMaintained("target/classes"));
    }

    @Test
    void theInnerClassFollowsItsSourceFile() throws IOException {
        plantOther();
        plant("target/generated-sources/annotations/" + PACKAGE_DIR);

        JavaClasses imported = importFrom("target/classes", RelocatedFixture.class, RelocatedFixture.Inner.class);

        assertFalse(SourceScope.isMaintained(imported.get(RelocatedFixture.Inner.class), List.of()));
    }

    @Test
    void aGeneratedFileAtThePackagePathWinsOverAMaintainedNamesakeElsewhere() throws IOException {
        plant("src/main/java/com/acme/domain");
        plant("target/generated-sources/openapi/src/main/java/" + PACKAGE_DIR);

        assertFalse(fixtureIsMaintained("target/classes"));
    }

    @Test
    void aMaintainedFileInADirectoryNotMatchingItsPackageIsStillMaintained() throws IOException {
        plant("src/main/kotlin/source");

        assertTrue(fixtureIsMaintained("target/classes"));
    }

    @Test
    void aClassWhoseRecordedSourceIsFoundNowhereIsChecked() throws IOException {
        plantOther();

        assertTrue(fixtureIsMaintained("target/classes"));
    }

    @Test
    void aClassRecordingNoSourceFileThatMatchesNothingIsGenerated() throws IOException {
        plantOther();
        JavaClass compiled = compileWithoutSourceFileAttribute("scratch");
        Files.delete(module.resolve("scratch/com/acme/NoDebugInfo.java"));

        assertFalse(SourceScope.isMaintained(compiled, List.of()));
    }

    @Test
    void aGeneratedFileInADirectoryNotMatchingItsPackageIsGenerated() throws IOException {
        plantOther();
        plant("target/generated-sources/kotlin/flat");

        assertFalse(fixtureIsMaintained("target/classes"));
    }

    @Test
    void aConfiguredDirectoryOutsideTheModuleHoldsGeneratedSources() throws IOException {
        Files.createFile(Files.createDirectories(module.resolve("app/src/main/java/com/acme")).resolve("Other.java"));
        plant("proto/build/generated/source/proto/main/java/" + PACKAGE_DIR);
        JavaClass fixture = importFrom("app/target/classes", RelocatedFixture.class).get(RelocatedFixture.class);

        assertFalse(SourceScope.isMaintained(fixture, List.of("../proto/build/generated")));
        assertTrue(SourceScope.isMaintained(fixture, List.of()), "found nowhere without the setting, so checked");
    }

    @Test
    void aConfiguredDirectoryHoldsGeneratedSources() throws IOException {
        plantOther();
        plant("src/main/generated/" + PACKAGE_DIR);
        JavaClass fixture = importFrom("target/classes", RelocatedFixture.class).get(RelocatedFixture.class);

        assertFalse(SourceScope.isMaintained(fixture, List.of("src/main/generated")));
        assertTrue(SourceScope.isMaintained(fixture, List.of("src/main/other")));
    }

    @Test
    void aSourceCorralCannotSeeIsStillChecked() throws IOException {
        plantOther();
        Files.createFile(Files.createDirectories(module.resolve("child")).resolve("pom.xml"));
        plant("child/src/main/java/" + PACKAGE_DIR);
        plant(".history/src/main/java/" + PACKAGE_DIR);

        assertTrue(fixtureIsMaintained("target/classes"));
    }

    @Test
    void aModuleWithoutAnySourceFileIsCheckedWhole() throws IOException {
        assertTrue(fixtureIsMaintained("build/classes/java/main"));
    }

    @Test
    void aClassOutsideARecognisedOutputIsChecked() throws IOException {
        plantOther();

        assertTrue(fixtureIsMaintained("lib/classes"));
    }

    @Test
    void aClassFromAJarIsChecked() {
        JavaClass fromJar = new ClassFileImporter().importClass(Test.class);

        assertTrue(SourceScope.isMaintained(fromJar, List.of()), fromJar.getSource()::toString);
    }

    @Test
    void aFilteredRuleSeesOnlyMaintainedClasses() throws IOException {
        plantOther();
        plant("target/generated-sources/openapi/" + PACKAGE_DIR);
        JavaClasses imported = importFrom("target/classes", RelocatedFixture.class);
        List<String> printed = new ArrayList<>();

        ArchRule filtered =
                SourceScope.maintainedOnly(NAMING_THE_FIXTURE, List.of(), new AtomicBoolean(), printed::add);

        assertFalse(filtered.evaluate(imported).hasViolation());
        assertTrue(NAMING_THE_FIXTURE.evaluate(imported).hasViolation(), "the unfiltered rule must flag the fixture");
        assertEquals(1, printed.size(), printed::toString);
        assertTrue(printed.get(0).contains("1 generated class"), printed::toString);
    }

    @Test
    void aFilteredRuleStillFlagsMaintainedClasses() throws IOException {
        plant("src/main/java/" + PACKAGE_DIR);
        List<String> printed = new ArrayList<>();

        ArchRule filtered =
                SourceScope.maintainedOnly(NAMING_THE_FIXTURE, List.of(), new AtomicBoolean(), printed::add)
                        .as("renamed").because("kept");

        assertTrue(filtered.evaluate(importFrom("target/classes", RelocatedFixture.class)).hasViolation());
        assertEquals("renamed, because kept", filtered.getDescription());
        assertEquals(List.of(), printed, "nothing was skipped, so nothing is printed");
    }

    @Test
    void theSkippedCountIsPrintedOncePerRun() throws IOException {
        plantOther();
        plant("target/generated-sources/annotations/" + PACKAGE_DIR);
        JavaClasses imported = importFrom("target/classes", RelocatedFixture.class);
        AtomicBoolean printedThisRun = new AtomicBoolean();
        List<String> printed = new ArrayList<>();

        SourceScope.maintainedOnly(NAMING_THE_FIXTURE, List.of(), printedThisRun, printed::add).evaluate(imported);
        SourceScope.maintainedOnly(NAMING_THE_FIXTURE, List.of(), printedThisRun, printed::add).evaluate(imported);

        assertEquals(1, printed.size(), printed::toString);
    }

    @Test
    void aClassCompiledWithoutTheSourceFileAttributeIsFoundByItsTopLevelName() throws IOException {
        JavaClass compiled = compileWithoutSourceFileAttribute("src/main/java");

        assertTrue(compiled.getSource().flatMap(Source::getFileName).isEmpty(),
                "precondition: -g:lines,vars must drop the SourceFile attribute");
        assertTrue(SourceScope.isMaintained(compiled, List.of()));
    }

    @Test
    void aClassCompiledWithoutTheSourceFileAttributeFromGeneratedSourcesIsGenerated() throws IOException {
        plantOther();

        assertFalse(SourceScope.isMaintained(compileWithoutSourceFileAttribute("target/generated-sources/x"),
                List.of()));
    }

    @Test
    void symlinkedSourcesAreFollowed(@TempDir Path elsewhere) throws IOException {
        Path realSources = Files.createDirectories(elsewhere.resolve("java/" + PACKAGE_DIR));
        Files.createFile(realSources.resolve(SOURCE_FILE));
        Files.createDirectories(module.resolve("src/main"));
        Files.createSymbolicLink(module.resolve("src/main/java"), elsewhere.resolve("java"));
        Files.createSymbolicLink(module.resolve("src/main/loop"), module.resolve("src"));

        assertTrue(fixtureIsMaintained("target/classes"));
    }

    @Test
    void anUnreadableDirectoryIsSkippedRatherThanFailingEveryRule() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        plant("src/main/java/" + PACKAGE_DIR);
        Path locked = Files.createDirectories(module.resolve("docker-data/pg"));
        Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("---------"));
        try {
            assertTrue(fixtureIsMaintained("target/classes"));
        } finally {
            Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("rwx------"));
        }
    }

    @Test
    void aConfiguredDotDirectoryIsIndexedAsGeneratedWithoutSwallowingTheRestOfTheModule() throws IOException {
        Files.createFile(Files.createDirectories(module.resolve("src/main/java/com/acme")).resolve("Other.java"));
        plant(".apt_generated/" + PACKAGE_DIR);
        JavaClass fixture = importFrom("target/classes", RelocatedFixture.class).get(RelocatedFixture.class);
        JavaClass other = compileInto("src/main/java", "com.acme", "Other");

        assertFalse(SourceScope.isMaintained(fixture, List.of(".apt_generated")));
        assertTrue(SourceScope.isMaintained(other, List.of(".apt_generated")));
    }

    @Test
    void aConfiguredDirectoryHoldingABuildFileIsIndexedAsGenerated() throws IOException {
        plantOther();
        plant("generated/" + PACKAGE_DIR);
        Files.createFile(module.resolve("generated/pom.xml"));

        assertFalse(SourceScope.isMaintained(
                importFrom("target/classes", RelocatedFixture.class).get(RelocatedFixture.class),
                List.of("generated")));
    }

    @Test
    void aGeneratedClassInTheDefaultPackageIsGenerated() throws IOException {
        plantOther();
        JavaClass lexer = compileInto("target/generated-sources/antlr4", "", "GrammarLexer");

        assertFalse(SourceScope.isMaintained(lexer, List.of()));
    }

    @Test
    void aMaintainedClassInTheDefaultPackageIsMaintained() throws IOException {
        JavaClass main = compileInto("src/main/java", "", "Main");

        assertTrue(SourceScope.isMaintained(main, List.of()));
    }

    @Test
    void aConfiguredDirectoryResolvingOntoTheModulesSourceRootFailsLoudly() throws IOException {
        Path child = Files.createDirectories(module.resolve("parent/child"));
        Files.createFile(Files.createDirectories(child.resolve("src/main/java/com/acme")).resolve("Other.java"));
        JavaClass fixture =
                importFrom("parent/child/target/classes", RelocatedFixture.class).get(RelocatedFixture.class);

        List<String> backOntoItsOwnSources = List.of("../../parent/child/src/main/java");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> SourceScope.isMaintained(fixture, backOntoItsOwnSources));

        assertTrue(thrown.getMessage().contains("../../parent/child/src/main/java"), thrown.getMessage());
    }

    @Test
    void aModuleHoldingOnlyGeneratedSourcesSkipsThem() throws IOException {
        Files.writeString(module.resolve("pom.xml"), "<project/>");
        plant("target/generated-sources/openapi/" + PACKAGE_DIR);

        assertFalse(fixtureIsMaintained("target/classes"));
    }

    @Test
    void nodeModulesIsNotTheModulesSources() throws IOException {
        plantOther();
        plant("target/generated-sources/openapi/flat");
        plant("node_modules/some-package/" + PACKAGE_DIR);

        assertFalse(fixtureIsMaintained("target/classes"));
    }

    @Test
    void pinsThePredicateDescription() {
        assertEquals("maintained classes", SourceScope.MAINTAINED.getDescription());
    }

    private boolean fixtureIsMaintained(String output) throws IOException {
        return SourceScope.isMaintained(importFrom(output, RelocatedFixture.class).get(RelocatedFixture.class),
                List.of());
    }

    private void plant(String directory) throws IOException {
        Files.createFile(Files.createDirectories(module.resolve(directory)).resolve(SOURCE_FILE));
    }

    private void plantOther() throws IOException {
        Path other = Files.createDirectories(module.resolve("src/main/java/com/acme"));
        Files.createFile(other.resolve("Other.java"));
    }

    /**
     * {@code packageName.simpleName}, its source planted under {@code sourceRoot}, compiled to
     * {@code target/classes}.
     */
    private JavaClass compileInto(String sourceRoot, String packageName, String simpleName) throws IOException {
        Path directory = Files.createDirectories(module.resolve(sourceRoot).resolve(packageName.replace('.', '/')));
        Path source = directory.resolve(simpleName + ".java");
        Files.writeString(source, (packageName.isEmpty() ? "" : "package " + packageName + "; ")
                + "public class " + simpleName + " {}");
        Path output = Files.createDirectories(module.resolve("target/classes"));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", output.toString(), source.toString()), "javac failed");
        String name = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
        return new ClassFileImporter().importPath(output).get(name);
    }

    /** {@code com.acme.NoDebugInfo}, its source planted under {@code sourceRoot}. */
    private JavaClass compileWithoutSourceFileAttribute(String sourceRoot) throws IOException {
        Path source = Files.createDirectories(module.resolve(sourceRoot + "/com/acme")).resolve("NoDebugInfo.java");
        Files.writeString(source, "package com.acme; public class NoDebugInfo {}");
        Path output = Files.createDirectories(module.resolve("target/classes"));
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-g:lines,vars", "-d", output.toString(), source.toString());
        assertEquals(0, exit, "javac failed");
        return new ClassFileImporter().importPath(output).get("com.acme.NoDebugInfo");
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
