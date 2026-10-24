package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;

/** MUST FLAG {@code org.springframework.boot.webtestclient..}: Boot 4's home for {@code WebTestClient} support. */
@AutoConfigureWebTestClient
public class StorefrontRehearsal {

    String describe() {
        return getClass().getSimpleName();
    }
}
