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

    /**
     * The escape hatches a reader can only reach by exact name: a file name and two property keys.
     * Pinned verbatim — paraphrasing one out of the policy would silently un-name it.
     */
    @Test
    void namesEveryEscapeHatchVerbatim() {
        String all = String.join("\n", AntiFixPolicy.clauses());

        assertTrue(all.contains("archunit/frozen/"), "must forbid editing the freeze store");
        assertTrue(all.contains("archunit_ignore_patterns.txt"), "must forbid the ignore-patterns file");
        assertTrue(all.contains("archunit.freeze.refreeze"), "must forbid refreezing");
        assertTrue(all.contains("freeze.store.default.allowStoreCreation"),
                "must forbid committing store creation");
    }

    @Test
    void baselineEndsWithTheOnlyAcceptableResolution() {
        String last = AntiFixPolicy.clauses().get(AntiFixPolicy.clauses().size() - 1).toLowerCase();

        assertTrue(last.contains("only"), "final clause must state the only acceptable resolution");
        assertTrue(last.contains("genuinely passes"), "final clause: " + last);
    }
}
