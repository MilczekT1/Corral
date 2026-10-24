package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import org.springframework.boot.resttestclient.TestRestTemplate;

/** MUST FLAG {@code org.springframework.boot.resttestclient..}: Boot 4's home for {@code TestRestTemplate}. */
public class PartnerApiClient {

    private final TestRestTemplate http = new TestRestTemplate();

    String fetchStatus(String url) {
        return http.getForObject(url, String.class);
    }
}
