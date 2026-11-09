package io.github.milczekt1.corral.scope;

import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.LocationProvider;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Scans the build output of the module the test class was compiled in — its test and production
 * output — and no sibling module or jar, whether the test runs under {@code test}, {@code verify},
 * Failsafe or an IDE.
 *
 * <pre>{@code @AnalyzeClasses(locations = CurrentModuleLocations.class)}</pre>
 *
 * <p>Do not add {@code packages} or {@code packagesOf} alongside it: {@code @AnalyzeClasses} imports
 * them in addition to these locations, which brings the sibling modules back.
 *
 * <p>Do not add {@code ImportOption.DoNotIncludeTests}: the rules scoped to test classes then pass
 * vacuously.
 *
 * <p>Recognises Maven ({@code target/test-classes}), Gradle's {@code test} source set
 * ({@code build/classes/<lang>/test}) and IntelliJ ({@code out/test/<module>}), and throws for any
 * other layout, such as a Gradle {@code integrationTest} source set; that needs its own
 * {@link LocationProvider}.
 */
public final class CurrentModuleLocations implements LocationProvider {

    @Override
    public Set<Location> get(Class<?> testClass) {
        Path testOutput = codeSourceOf(testClass);
        if (!Files.isDirectory(testOutput)) {
            throw new IllegalStateException(testClass.getName()
                    + " must be loaded from a module's test output directory, but was loaded from " + testOutput);
        }
        Set<Location> locations = new LinkedHashSet<>();
        for (Path output : OutputLayout.moduleOutputsOf(testOutput)) {
            locations.add(Location.of(output));
        }
        return locations;
    }

    private static Path codeSourceOf(Class<?> testClass) {
        CodeSource codeSource = testClass.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new IllegalStateException(testClass.getName() + " has no code source to derive a module from");
        }
        try {
            return Path.of(codeSource.getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot resolve the code source of " + testClass.getName(), e);
        }
    }
}
