package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import io.restassured.RestAssured;
import java.net.URI;

/** MUST FLAG {@code io.restassured..}: REST Assured as a production HTTP client. */
public class HealthProbeClient {

    int probe(URI endpoint) {
        return RestAssured.get(endpoint).statusCode();
    }
}
