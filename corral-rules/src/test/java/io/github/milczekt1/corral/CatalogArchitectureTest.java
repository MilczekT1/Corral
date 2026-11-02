package io.github.milczekt1.corral;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class CatalogArchitectureTest {

    private static final JavaClasses CATALOG = new ClassFileImporter().importPackages("io.github.milczekt1.corral");

    /** Main and test classes alike, so one rule's test borrowing another rule's fixtures fails too. */
    @Test
    void rulesDoNotDependOnEachOther() {
        slices().matching("io.github.milczekt1.corral.rules.(*).(*)..")
                .should().notDependOnEachOther()
                .check(CATALOG);
    }
}
