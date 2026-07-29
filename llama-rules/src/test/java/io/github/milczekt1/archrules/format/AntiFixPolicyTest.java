package io.github.milczekt1.archrules.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

    @Test
    void baselineEndsWithTheOnlyAcceptableResolution() {
        List<String> clauses = AntiFixPolicy.clauses();
        String last = clauses.get(clauses.size() - 1).toLowerCase();

        assertTrue(last.contains("only"), "final clause must state the only acceptable resolution");
        assertTrue(last.contains("genuinely passes"), "final clause: " + last);
    }

    @Test
    void addedClausesAppendAfterTheBaselineAndNeverReplaceIt() {
        List<String> baseline = AntiFixPolicy.clauses();

        AntiFixPolicy.addClause("Do NOT disable the module in CI.");
        List<String> extended = AntiFixPolicy.clauses();

        assertEquals(baseline.size() + 1, extended.size());
        assertEquals(baseline, extended.subList(0, baseline.size()), "baseline must be preserved verbatim");
        assertEquals("Do NOT disable the module in CI.", extended.get(extended.size() - 1));
    }

    @Test
    void clausesIsUnmodifiableSoCallersCannotStripTheBaseline() {
        assertThrows(UnsupportedOperationException.class, () -> AntiFixPolicy.clauses().clear());
    }

    @Test
    void rejectsBlankClauses() {
        assertThrows(IllegalArgumentException.class, () -> AntiFixPolicy.addClause("  "));
        assertThrows(IllegalArgumentException.class, () -> AntiFixPolicy.addClause(null));
    }
}
