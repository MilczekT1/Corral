package com.example.consumer;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.archrules.groups.AllCentralRules;

/**
 * The entire consumer-side wiring. No rule logic lives here.
 *
 * <p>Run granularities: the whole class, one group node, or a single rule leaf.
 *
 * <p>Note the absence of {@code ImportOption.DoNotIncludeTests}: {@code TestingRules} inspects test
 * classes, and excluding them would make those rules pass vacuously.
 */
@AnalyzeClasses(packages = "com.example.consumer", importOptions = ImportOption.DoNotIncludeJars.class)
class CentralArchitectureTest {

    @ArchTest
    static final ArchTests all = ArchTests.in(AllCentralRules.class);

    // Equivalent, if you prefer to opt in group by group:
    // @ArchTest static final ArchTests testing = ArchTests.in(TestingRules.class);
}
