package io.github.milczekt1.corral.rules.testsupport;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.milczekt1.corral.scope.TestScope;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

/**
 * Copies compiled fixtures out of test output, where {@link TestScope} classifies every one of them
 * as a test class, into a directory it classifies as production.
 */
@UtilityClass
public class ProductionLayout {

    /** Gradle's main output: matched by none of {@code TestScope}'s test-output patterns. */
    public static final String MAIN_OUTPUT = "build/classes/java/main";

    /**
     * Copies each class file, with its nested and anonymous classes, into
     * {@code root/}{@value #MAIN_OUTPUT}, keeping package directories.
     *
     * @return the copy's output root, to import from
     */
    public static Path copy(Path root, List<Class<?>> classes) throws IOException {
        Path output = root.resolve(MAIN_OUTPUT);
        for (Class<?> type : classes) {
            String binaryName = binaryNameOf(type);
            copyMatching(directoryOf(type), destinationFor(output, type),
                    name -> name.equals(binaryName + ".class") || name.startsWith(binaryName + "$"));
        }
        return output;
    }

    /**
     * Copies every class file in {@code anchor}'s package directory whose file name passes
     * {@code include} into {@code root/}{@value #MAIN_OUTPUT}. Sub-packages are not copied.
     *
     * @return the copy's output root, to import from
     */
    public static Path copyPackageOf(Class<?> anchor, Path root, Predicate<String> include) throws IOException {
        Path output = root.resolve(MAIN_OUTPUT);
        copyMatching(directoryOf(anchor), destinationFor(output, anchor), include);
        return output;
    }

    public static URL classFileOf(Class<?> type) {
        URL classFile = type.getResource(binaryNameOf(type) + ".class");
        assertNotNull(classFile, () -> "no compiled class file on the classpath for " + type.getName());
        return classFile;
    }

    public static Path directoryOf(Class<?> type) {
        URL classFile = classFileOf(type);
        try {
            return Path.of(classFile.toURI()).getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(classFile.toString(), e);
        }
    }

    private static void copyMatching(Path source, Path destination, Predicate<String> include) throws IOException {
        Files.createDirectories(destination);
        try (Stream<Path> classFiles = Files.list(source)) {
            for (Path classFile : classFiles
                    .filter(Files::isRegularFile)
                    .filter(file -> include.test(file.getFileName().toString()))
                    .toList()) {
                Files.copy(classFile, destination.resolve(classFile.getFileName()));
            }
        }
    }

    private static Path destinationFor(Path output, Class<?> type) {
        return output.resolve(type.getPackageName().replace('.', '/'));
    }

    /** {@code Outer$Inner} for a nested class, which is also its class file's name. */
    private static String binaryNameOf(Class<?> type) {
        return type.getName().substring(type.getPackageName().length() + 1);
    }
}
