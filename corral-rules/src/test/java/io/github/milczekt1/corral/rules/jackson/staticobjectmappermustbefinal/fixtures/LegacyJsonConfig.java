package io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

/** MUST FLAG {@code MAPPER}: a container writing a static field through a setter leaves it non-final. */
public class LegacyJsonConfig {

    private static ObjectMapper MAPPER;

    @Autowired
    void setMapper(ObjectMapper mapper) {
        MAPPER = mapper;
    }

    static ObjectMapper mapper() {
        return MAPPER;
    }
}
