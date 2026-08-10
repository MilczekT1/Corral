package com.example.consumer.custom;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

/** Wires this project's own rules, alongside the library's in {@code CentralArchitectureTest}. */
@AnalyzeClasses(packages = "com.example.consumer", importOptions = ImportOption.DoNotIncludeJars.class)
class CustomArchitectureTest {

    @ArchTest
    static final ArchTests custom = ArchTests.in(NoStdoutInServices.class);
}
