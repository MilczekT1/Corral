package io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures;

import org.springframework.beans.factory.annotation.Value;

/** MUST FLAG: named like a test but declares no test method, so in a production layout it is production. */
public class PortSmokeTest {

    @Value("${local.server.port}")
    private int port;

    public boolean listening() {
        return port > 0;
    }
}
