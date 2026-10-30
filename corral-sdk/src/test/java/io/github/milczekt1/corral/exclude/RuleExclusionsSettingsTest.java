package io.github.milczekt1.corral.exclude;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.corral.exclude.RuleExclusions.Loaded;
import io.github.milczekt1.corral.settings.SettingsFile;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Where exclusions come from; {@code SettingsFileTest} owns what the file may say. */
class RuleExclusionsSettingsTest {

    @Test
    void exclusionsComeFromTheSettingsFile() {
        SettingsFile settings = new SettingsFile(
                Map.of("corral.exclude.a.no-b", "why"), "file:/somewhere/corral.properties", List.of(), null);

        Loaded loaded = RuleExclusions.from(settings);

        assertFalse(loaded.isBroken(), loaded::problem);
        assertEquals(List.of(new Exclusion("a.no-b", "why")), loaded.entries());
    }

    @Test
    void aBrokenSettingsFileBreaksExclusions() {
        Loaded loaded = RuleExclusions.from(SettingsFile.broken("boom"));

        assertTrue(loaded.isBroken());
        assertEquals("boom", loaded.problem());
        assertTrue(loaded.entries().isEmpty(), () -> "a broken file must exclude nothing: " + loaded);
    }

    @Test
    void noSettingsFileMeansNoExclusions() {
        Loaded loaded = RuleExclusions.from(SettingsFile.none());

        assertFalse(loaded.isBroken());
        assertTrue(loaded.entries().isEmpty());
    }

    @Test
    void anExclusionCannotBeBuiltWithoutAWellFormedIdAndAReason() {
        assertThrows(IllegalArgumentException.class, () -> new Exclusion(null, "a reason"));
        assertThrows(IllegalArgumentException.class, () -> new Exclusion("logging.no-system-out", null));
        assertThrows(IllegalArgumentException.class, () -> new Exclusion("logging.no-system-out", "  "));
        assertThrows(IllegalArgumentException.class, () -> new Exclusion("Not An Id", "a reason"));
    }

    @Test
    void censusLinesReadAsTheyAreWrittenInTheFile() {
        Loaded loaded = Loaded.of(List.of(new Exclusion("a.no-b", "why")));

        assertEquals(List.of("corral.exclude.a.no-b=why"), RuleExclusions.census(loaded));
    }

    /** The absent direction on this build's own classpath, through the memoised scan. */
    @Test
    void thisBuildCarriesNoExclusions() {
        assertTrue(RuleExclusions.inEffect().isEmpty(),
                "this module must not commit a corral.properties, or every test here proves less");
        assertFalse(RuleExclusions.isBroken());
    }
}
