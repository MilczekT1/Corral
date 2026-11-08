package com.example.consumer.wiring;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.consumer.service.GreetingService;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.github.milczekt1.corral.scope.CurrentModuleLocations;
import org.junit.jupiter.api.Test;

/** Failsafe puts this module's own jar on the classpath, where {@code DoNotIncludeJars} drops it. */
class ModuleScanIT {

    @Test
    void failsafeLoadsThisModuleFromItsJar() {
        String codeSource = GreetingService.class.getProtectionDomain().getCodeSource().getLocation().toString();

        assertTrue(codeSource.endsWith(".jar"),
                "Failsafe no longer puts this module's jar on the classpath; ModuleScanIT proves nothing: " + codeSource);
    }

    @Test
    void theModulesProductionClassesAreScannedAnyway() {
        JavaClasses imported = new ClassFileImporter()
                .importLocations(new CurrentModuleLocations().get(ModuleScanIT.class));

        assertTrue(imported.contain(GreetingService.class), "the module's production classes were not scanned");
    }
}
