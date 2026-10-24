package io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.fixtures;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

/** MUST FLAG {@code codecMapper}, a Jackson 3 {@code JsonMapper}; MUST IGNORE the {@code static final} Jackson 3 mapper and the Jackson 3 reader. */
public class EventCodec {

    private static final ObjectMapper SHARED = JsonMapper.builder().build();

    private static JsonMapper codecMapper = JsonMapper.builder().build();

    private static ObjectReader eventReader = SHARED.readerFor(String.class);

    static String encode(Object event) {
        return codecMapper.writeValueAsString(event) + SHARED.writeValueAsString(event);
    }

    static String decode(String json) {
        return eventReader.readValue(json);
    }
}
