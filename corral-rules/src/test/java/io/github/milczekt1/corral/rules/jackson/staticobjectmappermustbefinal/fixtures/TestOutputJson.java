package io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;

/** MUST IGNORE from test output; MUST FLAG {@code overrideMapper} once copied into a production layout. */
public class TestOutputJson {

    static ObjectMapper overrideMapper = new ObjectMapper();

    static String write(Object value) throws Exception {
        return overrideMapper.writeValueAsString(value);
    }
}
