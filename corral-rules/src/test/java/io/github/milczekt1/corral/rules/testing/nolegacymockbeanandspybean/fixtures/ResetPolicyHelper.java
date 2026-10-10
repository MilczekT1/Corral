package io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean.fixtures;

import org.springframework.boot.test.mock.mockito.MockReset;

/** MUST FLAG: a support type from the removed package, used with no annotation in sight. */
public class ResetPolicyHelper {

    Object policy() {
        return MockReset.AFTER;
    }
}
