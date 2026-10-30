package io.github.milczekt1.corral.rules.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tngtech.archunit.core.importer.Location;
import io.github.milczekt1.corral.scope.TestScope;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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
            copyMatching(directoryOf(type), destinationFor(output, type), belongsTo(type));
        }
        return output;
    }

    /**
     * Copies every class file in {@code anchor}'s package directory into
     * {@code root/}{@value #MAIN_OUTPUT}, except {@code excluded} and their nested and anonymous
     * classes. Sub-packages are not copied.
     *
     * @return the copy's output root, to import from
     */
    public static Path copyPackageOf(Class<?> anchor, Path root, List<Class<?>> excluded) throws IOException {
        return copyPackageOf(anchor, root, MAIN_OUTPUT, excluded);
    }

    /** As {@link #copyPackageOf(Class, Path, List)}, into {@code root/layout} instead. */
    public static Path copyPackageOf(Class<?> anchor, Path root, String layout, List<Class<?>> excluded)
            throws IOException {
        Path output = root.resolve(layout);
        Predicate<String> isExcluded = excluded.stream().map(ProductionLayout::belongsTo)
                .reduce(name -> false, Predicate::or);
        copyMatching(directoryOf(anchor), destinationFor(output, anchor), isExcluded.negate());
        return output;
    }

    /**
     * {@code productionOutput}, plus each test-output type's class file and its nested and anonymous
     * classes where they compiled.
     */
    public static List<Location> withTestOutput(Path productionOutput, Class<?>... testOutputTypes)
            throws IOException {
        List<Location> locations = new ArrayList<>(List.of(Location.of(productionOutput)));
        for (Class<?> type : testOutputTypes) {
            for (Path classFile : classFilesIn(directoryOf(type), belongsTo(type))) {
                locations.add(Location.of(classFile));
            }
        }
        return locations;
    }

    /** Every class file in {@code anchor}'s package directory, sub-packages excluded. */
    public static List<Path> classFilesInPackageOf(Class<?> anchor) throws IOException {
        return classFilesIn(directoryOf(anchor), name -> name.endsWith(".class"));
    }

    public static Path directoryOf(Class<?> type) {
        URL classFile = type.getResource(binaryNameOf(type) + ".class");
        assertNotNull(classFile, () -> "no compiled class file on the classpath for " + type.getName());
        assertEquals("file", classFile.getProtocol(),
                () -> "fixtures are copied by listing their directory, so they must load from one: " + classFile);
        try {
            return Path.of(classFile.toURI()).getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(classFile.toString(), e);
        }
    }

    private static void copyMatching(Path source, Path destination, Predicate<String> include) throws IOException {
        Files.createDirectories(destination);
        for (Path classFile : classFilesIn(source, include)) {
            // Overlapping inputs, such as Outer and Outer.Inner, copy the same file twice.
            Files.copy(classFile, destination.resolve(classFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static List<Path> classFilesIn(Path directory, Predicate<String> include) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(file -> include.test(file.getFileName().toString()))
                    .toList();
        }
    }

    /** Matches the class file of {@code type} and of every class nested in it, at any depth. */
    private static Predicate<String> belongsTo(Class<?> type) {
        String binaryName = binaryNameOf(type);
        return name -> name.equals(binaryName + ".class") || name.startsWith(binaryName + "$");
    }

    private static Path destinationFor(Path output, Class<?> type) {
        return output.resolve(type.getPackageName().replace('.', '/'));
    }

    /** {@code Outer$Inner} for a nested class, which is also its class file's name. */
    private static String binaryNameOf(Class<?> type) {
        String packageName = type.getPackageName();
        return packageName.isEmpty() ? type.getName() : type.getName().substring(packageName.length() + 1);
    }
}
