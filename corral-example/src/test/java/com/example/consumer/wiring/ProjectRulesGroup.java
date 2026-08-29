package com.example.consumer.wiring;

import com.example.consumer.custom.NoStdoutInServicesRule;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.groups.TestingRulesGroup;

/**
 * This project's own catalog root: the central groups it opted into, plus the rules it wrote itself,
 * as one node. Corral publishes no such root — what runs here is a decision this repo records, so no
 * upgrade can widen it.
 *
 * <p>Deliberately not every central group: {@code LoggingRulesGroup} is wired by
 * {@code com.example.consumer.exclusions.ExcludedRuleTest} instead, which is what a real project
 * does when one area needs its own scope or its own exclusions. Adding a group is a one-line edit
 * here, reviewed like any other change.
 *
 * <p>Members are {@code ArchTests} fields whatever they aggregate — a central group, your own group,
 * or a single rule class — and {@code ArchTests.in(...)} descends through all of them.
 */
final class ProjectRulesGroup {

    @ArchTest
    static final ArchTests testing = ArchTests.in(TestingRulesGroup.class);

    @ArchTest
    static final ArchTests ownRules = ArchTests.in(NoStdoutInServicesRule.class);

    private ProjectRulesGroup() {
    }
}
