package io.github.milczekt1.corral.rules.java.nopublicstaticarrayfields.fixtures;

/** MUST IGNORE from test output; MUST FLAG {@code FIXTURE_IDS} once copied into a production layout. */
public class TestOutputArrays {

    public static final String[] FIXTURE_IDS = {"order-1", "order-2"};

    public static int count() {
        return FIXTURE_IDS.length;
    }
}
