package io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal.fixtures;

/** MUST IGNORE from test output; MUST FLAG {@code overrideUrl} once copied into a production layout. */
public class TestOutputSettings {

    public static String overrideUrl = "http://localhost:8080";

    public static String url() {
        return overrideUrl;
    }
}
