package io.github.milczekt1.corral.rules.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FixtureVerdictsTest {

    private static final Path RULE_TESTS = FixtureVerdicts.TEST_SOURCES.resolve("io/github/milczekt1/corral/rules");

    @Test
    void everyFixtureOpensWithAVerdict() throws IOException {
        List<Path> fixtures = sourcesUnder(RULE_TESTS).stream()
                .filter(source -> source.toString().contains("/fixtures/"))
                .toList();
        assertFalse(fixtures.isEmpty(), "premise: the scan must find fixtures, or it checks nothing");

        assertEquals(List.of(), fixtures.stream().filter(source -> FixtureVerdicts.verdictOf(source).isEmpty()).toList(),
                "every fixture's type Javadoc must open with MUST FLAG, MUST IGNORE or SUPPORT");
    }

    @Test
    void everyRuleTestWithFixturesChecksTheirVerdicts() throws IOException {
        List<Path> ruleTests = sourcesUnder(RULE_TESTS).stream()
                .filter(source -> source.getFileName().toString().endsWith("RuleTest.java"))
                .filter(source -> Files.isDirectory(source.resolveSibling("fixtures")))
                .toList();
        assertFalse(ruleTests.isEmpty(), "premise: the scan must find rule tests, or it checks nothing");

        assertEquals(List.of(), ruleTests.stream().filter(source -> !callsAssertMatch(source)).toList(),
                "a rule test with fixtures must call FixtureVerdicts.assertMatch");
    }

    private static boolean callsAssertMatch(Path source) {
        try {
            return Files.readString(source).contains("FixtureVerdicts.assertMatch(");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Path> sourcesUnder(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(file -> file.toString().endsWith(".java")).sorted().toList();
        }
    }
}
