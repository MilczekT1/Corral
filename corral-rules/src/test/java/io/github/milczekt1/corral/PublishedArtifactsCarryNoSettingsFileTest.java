package io.github.milczekt1.corral;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.corral.settings.CorralSettings;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A {@code corral.properties} in a published jar would configure every consumer that has none of its own. */
class PublishedArtifactsCarryNoSettingsFileTest {

    @Test
    void neitherCorralSdkNorCorralRulesShipsASettingsFile() throws IOException {
        List<String> published = Collections.list(getClass().getClassLoader().getResources(CorralSettings.FILE))
                .stream()
                .map(URL::toString)
                .filter(url -> url.contains("/target/classes/")
                        || url.startsWith("jar:") && (url.contains("corral-sdk") || url.contains("corral-rules")))
                .toList();

        assertTrue(published.isEmpty(), () -> "published artifacts carry " + CorralSettings.FILE + ": " + published);
    }
}
