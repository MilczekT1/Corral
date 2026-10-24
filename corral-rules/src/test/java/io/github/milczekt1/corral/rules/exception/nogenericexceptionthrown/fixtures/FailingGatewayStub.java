package io.github.milczekt1.corral.rules.exception.nogenericexceptionthrown.fixtures;

/**
 * MUST IGNORE where it compiled: test output is out of scope. Its test copies it into a production
 * layout to show the same constructor call is flagged there.
 */
public class FailingGatewayStub {

    public String fetch(String key) {
        throw new RuntimeException("boom");
    }
}
