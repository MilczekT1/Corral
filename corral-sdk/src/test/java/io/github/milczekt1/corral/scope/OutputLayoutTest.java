package io.github.milczekt1.corral.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class OutputLayoutTest {

    @TempDir
    Path root;

    @Test
    void mavenAddsTheSiblingProductionOutput() throws IOException {
        Path testOutput = directories("target/test-classes", "target/classes").get(0);

        assertEquals(List.of(testOutput, root.resolve("target/classes")), OutputLayout.moduleOutputsOf(testOutput));
    }

    @Test
    void gradleAddsEveryLanguagesTestAndMainOutput() throws IOException {
        List<Path> outputs = directories("build/classes/java/test", "build/classes/kotlin/test",
                "build/classes/java/main", "build/classes/kotlin/main");

        List<Path> found = OutputLayout.moduleOutputsOf(outputs.get(0));

        assertEquals(outputs.get(0), found.get(0));
        assertEquals(Set.copyOf(outputs), Set.copyOf(found));
        assertEquals(outputs.size(), found.size(), found::toString);
    }

    @Test
    void gradleWithoutTheLanguageDirectory() throws IOException {
        List<Path> outputs = directories("build/classes/test", "build/classes/main");

        assertEquals(outputs, OutputLayout.moduleOutputsOf(outputs.get(0)));
    }

    @Test
    void intellijAddsTheMatchingProductionModule() throws IOException {
        List<Path> outputs = directories("out/test/app", "out/production/app", "out/production/other");

        assertEquals(outputs.subList(0, 2), OutputLayout.moduleOutputsOf(outputs.get(0)));
    }

    @ParameterizedTest(name = "{0} alone")
    @ValueSource(strings = {"target/test-classes", "build/classes/java/test", "build/classes/test", "out/test/app"})
    void aModuleWithoutProductionOutputYieldsTestOutputAlone(String layout) throws IOException {
        Path testOutput = directories(layout).get(0);

        assertEquals(List.of(testOutput), OutputLayout.moduleOutputsOf(testOutput));
    }

    @Test
    void aCustomGradleSourceSetIsRejected() throws IOException {
        Path testOutput = directories("build/classes/java/integrationTest", "build/classes/java/main").get(0);

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> OutputLayout.moduleOutputsOf(testOutput));

        assertTrue(thrown.getMessage().contains(testOutput.toString()), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("LocationProvider"), thrown.getMessage());
    }

    @ParameterizedTest(name = "{0} owns {1}")
    @CsvSource({
            "MAVEN, target/test-classes",
            "GRADLE, build/classes/java/test",
            "GRADLE, build/classes/test",
            "INTELLIJ, out/test/app"})
    void eachLayoutOwnsTheDirectoriesItsPatternMatches(OutputLayout layout, String path) throws IOException {
        Path testOutput = directories(path).get(0);
        String classUri = testOutput.toUri() + "com/acme/A.class";

        assertTrue(layout.testClassUri().matcher(classUri).matches(), classUri);
        assertTrue(layout.moduleOutputs(testOutput).isPresent(), path);
        Arrays.stream(OutputLayout.values())
                .filter(other -> other != layout)
                .forEach(other -> assertEquals(Optional.empty(), other.moduleOutputs(testOutput), other::name));
    }

    private List<Path> directories(String... paths) throws IOException {
        List<Path> created = Arrays.stream(paths).map(root::resolve).toList();
        for (Path directory : created) {
            Files.createDirectories(directory);
        }
        return created;
    }
}
