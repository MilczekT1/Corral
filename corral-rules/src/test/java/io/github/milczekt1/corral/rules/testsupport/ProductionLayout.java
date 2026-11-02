package io.github.milczekt1.corral.rules.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
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
     * Imports every example of {@code ruleTest}: {@code fixtures/production} copied into
     * {@code root/}{@value #MAIN_OUTPUT}, and the rest of {@code fixtures}, sub-packages excluded, from
     * test output where it compiled.
     */
    public static JavaClasses importFixtures(Class<?> ruleTest, Path root) throws IOException {
        Path fixtures = packageDirectoryOf(ruleTest, "fixtures");
        String productionPackage = ruleTest.getPackageName() + ".fixtures.production";
        Path output = root.resolve(MAIN_OUTPUT);
        copyMatching(fixtures.resolve("production"), output.resolve(productionPackage.replace('.', '/')),
                name -> name.endsWith(".class"));

        List<Location> locations = new ArrayList<>(List.of(Location.of(output)));
        for (Path classFile : classFilesIn(fixtures, name -> name.endsWith(".class"))) {
            locations.add(Location.of(classFile));
        }
        return new ClassFileImporter().importLocations(locations);
    }

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
     * {@code root/}{@value #MAIN_OUTPUT}. Sub-packages are not copied.
     *
     * @return the copy's output root, to import from
     */
    public static Path copyPackageOf(Class<?> anchor, Path root) throws IOException {
        return copyPackageOf(anchor, root, MAIN_OUTPUT);
    }

    /** As {@link #copyPackageOf(Class, Path)}, into {@code root/layout} instead. */
    public static Path copyPackageOf(Class<?> anchor, Path root, String layout) throws IOException {
        Path output = root.resolve(layout);
        copyMatching(directoryOf(anchor), destinationFor(output, anchor), name -> name.endsWith(".class"));
        return output;
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

    private static Path packageDirectoryOf(Class<?> anchor, String relativePackage) {
        URL directory = anchor.getResource(relativePackage);
        assertNotNull(directory, () -> "no compiled package " + relativePackage + " beside " + anchor.getName());
        assertEquals("file", directory.getProtocol(),
                () -> "fixtures are copied by listing their directory, so they must load from one: " + directory);
        try {
            return Path.of(directory.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(directory.toString(), e);
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
