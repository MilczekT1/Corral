package com.example.consumer.exclusions;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.groups.LoggingRulesGroup;

/**
 * The exclusion feature, on its own node: {@code LoggingRulesGroup} holds
 * {@code corral.logging.no-system-err}, which {@code src/test/resources/corral-exclusions.txt}
 * removes from this build. {@code StderrWriterAllowedByExclusion} is the class that would fail.
 *
 * <p>Wired here rather than from {@link com.example.consumer.wiring.ProjectRulesGroup} on purpose: a
 * rule id is the freeze-store key, so evaluating the same group from two nodes would have both
 * writing the same store entry.
 */
@AnalyzeClasses(packages = "com.example.consumer", importOptions = ImportOption.DoNotIncludeJars.class)
class ExcludedRuleTest {

    @ArchTest
    static final ArchTests logging = ArchTests.in(LoggingRulesGroup.class);
}
