package com.example.consumer.custom;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

@AnalyzeClasses(packages = "com.example.consumer", importOptions = ImportOption.DoNotIncludeJars.class)
class CustomArchitectureTest {

    @ArchTest
    static final ArchTests custom = ArchTests.in(NoStdoutInServicesRule.class);
}
