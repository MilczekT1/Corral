package io.github.milczekt1.corral.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurrentModuleLocationsTest {

    private static final Path TARGET = Path.of("target").toAbsolutePath();

    private final CurrentModuleLocations provider = new CurrentModuleLocations();

    @Test
    void returnsTheTestOutputAndItsProductionSibling() {
        List<Path> locations = provider.get(CurrentModuleLocationsTest.class).stream()
                .map(location -> Path.of(location.asURI()))
                .toList();

        assertEquals(List.of(TARGET.resolve("test-classes"), TARGET.resolve("classes")), locations);
    }

    @Test
    void importsProductionAndTestClassesOfThisModuleOnly() {
        JavaClasses imported = new ClassFileImporter().importLocations(provider.get(CurrentModuleLocationsTest.class));

        assertTrue(imported.contain(TestScope.class));
        assertTrue(imported.contain(CurrentModuleLocationsTest.class));
        assertFalse(imported.stream().anyMatch(javaClass -> javaClass.getPackageName().startsWith("org.junit")),
                "a jar on the classpath was imported");
    }

    @Test
    void rejectsAClassLoadedFromAJar() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> provider.get(Test.class));

        assertTrue(thrown.getMessage().contains("junit-jupiter-api"), thrown.getMessage());
    }

    @Test
    void rejectsAProductionClass() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> provider.get(TestScope.class));

        assertTrue(thrown.getMessage().contains("target/classes"), thrown.getMessage());
    }

    @Test
    void rejectsAClassWithoutACodeSource() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> provider.get(String.class));

        assertTrue(thrown.getMessage().contains("java.lang.String"), thrown.getMessage());
    }
}
