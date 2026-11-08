package io.github.milczekt1.corral.scope;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The build layouts whose test output Corral recognises, and where each keeps the same module's other
 * output.
 *
 * <p>The patterns are the ones {@code ImportOption.DoNotIncludeTests} matches source URIs against,
 * copied because {@code ImportOption.Predefined} takes a {@code Location}, not a {@code JavaClass}.
 * Editing one changes what every rule scoped through {@link TestScope} reports, across every
 * consumer's store.
 */
enum OutputLayout {

    MAVEN(".*/target/test-classes/.*") {
        @Override
        Optional<List<Path>> moduleOutputs(Path testOutput) {
            Path target = testOutput.getParent();
            if (!isNamed(testOutput, "test-classes") || !isNamed(target, "target")) {
                return Optional.empty();
            }
            return Optional.of(existing(testOutput, Stream.of(target.resolve("classes"))));
        }
    },

    GRADLE(".*/build/classes/([^/]+/)?test/.*") {
        @Override
        Optional<List<Path>> moduleOutputs(Path testOutput) {
            Path parent = testOutput.getParent();
            if (!isNamed(testOutput, "test") || parent == null) {
                return Optional.empty();
            }
            if (isClassesOfBuild(parent)) {
                return Optional.of(existing(testOutput, Stream.of(parent.resolve("main"))));
            }
            Path classes = parent.getParent();
            if (!isClassesOfBuild(classes)) {
                return Optional.empty();
            }
            Stream<Path> languages = directoriesIn(classes).stream();
            return Optional.of(existing(testOutput, languages.flatMap(
                    language -> Stream.of(language.resolve("test"), language.resolve("main")))));
        }
    },

    INTELLIJ(".*/out/test/.*") {
        @Override
        Optional<List<Path>> moduleOutputs(Path testOutput) {
            Path test = testOutput.getParent();
            if (!isNamed(test, "test") || !isNamed(test.getParent(), "out")) {
                return Optional.empty();
            }
            Path production = test.resolveSibling("production").resolve(testOutput.getFileName().toString());
            return Optional.of(existing(testOutput, Stream.of(production)));
        }
    };

    private final Pattern testClassUri;

    OutputLayout(String testClassUri) {
        this.testClassUri = Pattern.compile(testClassUri);
    }

    /** Matched against the source URI of a class file, not against a directory. */
    Pattern testClassUri() {
        return testClassUri;
    }

    /**
     * {@code testOutput} first, then the module's other output directories that exist; empty when
     * {@code testOutput} is not this layout's test output.
     */
    abstract Optional<List<Path>> moduleOutputs(Path testOutput);

    static List<Path> moduleOutputsOf(Path testOutput) {
        return Arrays.stream(values())
                .map(layout -> layout.moduleOutputs(testOutput))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(testOutput
                        + " is not a test output directory Corral recognises (Maven target/test-classes,"
                        + " Gradle build/classes/<lang>/test, IntelliJ out/test/<module>)."
                        + " Implement your own com.tngtech.archunit.junit.LocationProvider for this layout."));
    }

    private static boolean isNamed(Path path, String name) {
        return path != null && path.getFileName() != null && path.getFileName().toString().equals(name);
    }

    private static boolean isClassesOfBuild(Path path) {
        return isNamed(path, "classes") && isNamed(path.getParent(), "build");
    }

    private static List<Path> existing(Path testOutput, Stream<Path> candidates) {
        List<Path> outputs = new ArrayList<>(List.of(testOutput));
        candidates.filter(Files::isDirectory)
                .filter(candidate -> !candidate.equals(testOutput))
                .forEach(outputs::add);
        return outputs;
    }

    private static List<Path> directoriesIn(Path directory) {
        try (Stream<Path> children = Files.list(directory)) {
            return children.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
