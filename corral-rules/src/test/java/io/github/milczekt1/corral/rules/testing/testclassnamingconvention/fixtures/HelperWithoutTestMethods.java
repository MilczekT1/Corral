package io.github.milczekt1.corral.rules.testing.testclassnamingconvention.fixtures;

/**
 * MUST IGNORE: top-level and unconventionally named, but declares no JUnit test method, so no
 * naming verdict applies. Drop the rule's test-method clause and this class starts failing.
 */
public class HelperWithoutTestMethods {

    public String describe() {
        return "no build tool was ever going to run me, and that is correct";
    }
}
