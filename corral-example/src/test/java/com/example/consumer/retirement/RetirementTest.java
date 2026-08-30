package com.example.consumer.retirement;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

/**
 * Retirement on its own node: {@link DateRulesGroup} publishes the live rule and the retired id it
 * replaced. Running this class shows both, the retired one passing and naming its successor.
 */
@AnalyzeClasses(packages = "com.example.consumer", importOptions = ImportOption.DoNotIncludeJars.class)
class RetirementTest {

    @ArchTest
    static final ArchTests dates = ArchTests.in(DateRulesGroup.class);
}
