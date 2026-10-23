package io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures;

import org.springframework.beans.factory.annotation.Value;

/**
 * MUST IGNORE when imported from where it compiles, test output. MUST FLAG if copied into a
 * production layout, which is how the test shows the output location, not a test method, spares it.
 */
public class TestOutputPortHolder {

    @Value("${local.server.port}")
    private int port;

    public String baseUrl() {
        return "http://localhost:" + port;
    }
}
