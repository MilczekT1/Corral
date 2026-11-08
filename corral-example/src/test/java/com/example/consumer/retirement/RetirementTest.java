package com.example.consumer.retirement;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.scope.CurrentModuleLocations;

/**
 * Retirement on its own node: {@link DateRulesGroup} publishes the live rule and the retired id it
 * replaced. Running this class shows both, the retired one passing and naming its successor.
 */
@AnalyzeClasses(locations = CurrentModuleLocations.class)
class RetirementTest {

    @ArchTest
    static final ArchTests dates = ArchTests.in(DateRulesGroup.class);
}
