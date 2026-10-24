package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;

/** MUST FLAG {@code org.springframework.*.test..}: spring-security-test, one level below a project root. */
public class SystemUserRunner {

    void runAsSystem(Runnable job) {
        TestSecurityContextHolder.setAuthentication(new TestingAuthenticationToken("system", null));
        job.run();
    }
}
