package io.github.milczekt1.corral.format;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AntiFixPolicyTest {

    @Test
    void baselineCoversEveryDocumentedCheat() {
        String all = String.join("\n", AntiFixPolicy.clauses()).toLowerCase();

        assertTrue(all.contains("archunit/frozen"), "must forbid editing the freeze store");
        assertTrue(all.contains("@suppresswarnings"), "must forbid suppressing");
        assertTrue(all.contains("@archignore"), "must forbid @ArchIgnore");
        assertTrue(all.contains("rename"), "must forbid renaming to dodge name-based rules");
        assertTrue(all.contains("@analyzeclasses"), "must forbid narrowing the scan");
        assertTrue(all.contains("importoption"), "must forbid hiding code with ImportOptions");
        assertTrue(all.contains("weaken"), "must forbid weakening the rule");
    }

    /** The escape hatches a reader can only reach by exact name: a file name and two property keys. */
    @Test
    void namesEveryEscapeHatchVerbatim() {
        String all = String.join("\n", AntiFixPolicy.clauses());

        assertTrue(all.contains("archunit/frozen/"), "must forbid editing the freeze store");
        assertTrue(all.contains("archunit_ignore_patterns.txt"), "must forbid the ignore-patterns file");
        assertTrue(all.contains("archunit.freeze.refreeze"), "must forbid refreezing");
        assertTrue(all.contains("freeze.store.default.allowStoreCreation"),
                "must forbid committing store creation");
        assertTrue(all.contains("corral.ignorePatterns.fail"),
                "must forbid disarming the ignore-patterns check");
        assertTrue(all.contains("corral-exclusions.txt"),
                "must draw the line around the one subtractive mechanism, or it reads as unsanctioned");
    }

    /**
     * The exclusions clause carries its own limits: permanent, not applicable, not a way to pass,
     * and the same-change tell.
     */
    @Test
    void theExclusionsClauseDrawsTheLineRatherThanAdvertisingAnEscapeRoute() {
        String clause = AntiFixPolicy.clauses().stream()
                .filter(line -> line.contains("corral-exclusions.txt"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no clause mentions corral-exclusions.txt"));
        String text = clause.toLowerCase();

        assertTrue(text.contains("permanent"), clause);
        assertTrue(text.contains("does not apply"), clause);
        assertTrue(text.contains("not a way to pass a failing build"), clause);
        assertTrue(text.contains("same change"),
                "the abuse is adding a line in the change that made the rule fail: " + clause);
        assertTrue(text.contains("silencing"), clause);
    }

    @Test
    void baselineEndsWithTheOnlyAcceptableResolution() {
        String last = AntiFixPolicy.clauses().get(AntiFixPolicy.clauses().size() - 1).toLowerCase();

        assertTrue(last.contains("only"), "final clause must state the only acceptable resolution");
        assertTrue(last.contains("genuinely passes"), "final clause: " + last);
    }
}
