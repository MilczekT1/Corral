package io.github.milczekt1.corral;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class SdkPackageCyclesTest {

    private static final JavaClasses SDK = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.milczekt1.corral");

    /** One slice per package, so the root package is a slice too and cannot hide a cycle. */
    @Test
    void sdkPackagesAreFreeOfCycles() {
        slices().matching("io.github.milczekt1.(**)")
                .should().beFreeOfCycles()
                .check(SDK);
    }
}
