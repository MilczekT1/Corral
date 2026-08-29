package com.example.consumer.wiring;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

/**
 * Runs everything {@link ProjectRulesGroup} composes. No rule logic lives here.
 *
 * <p>Run granularities: the whole class, one group node, or a single rule leaf.
 *
 * <p>Note the absence of {@code ImportOption.DoNotIncludeTests}: {@code TestingRulesGroup} inspects
 * test classes, and excluding them would make those rules pass vacuously.
 */
@AnalyzeClasses(packages = "com.example.consumer", importOptions = ImportOption.DoNotIncludeJars.class)
class ProjectArchitectureTest {

    @ArchTest
    static final ArchTests all = ArchTests.in(ProjectRulesGroup.class);
}
