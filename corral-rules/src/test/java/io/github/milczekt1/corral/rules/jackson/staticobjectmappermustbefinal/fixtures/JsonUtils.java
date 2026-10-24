package io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.util.concurrent.atomic.AtomicReference;

/** MUST FLAG {@code mapper}, initialised lazily; MUST IGNORE the static reader and writer, the {@code AtomicReference} holder and the {@code Object}-typed field. */
public class JsonUtils {

    private static ObjectMapper mapper;

    private static ObjectReader reader;

    private static ObjectWriter prettyWriter;

    private static final AtomicReference<ObjectMapper> current = new AtomicReference<>(new ObjectMapper());

    private static Object untyped;

    static void init() {
        mapper = new ObjectMapper();
        reader = mapper.reader();
        prettyWriter = mapper.writerWithDefaultPrettyPrinter();
        untyped = current.get();
    }

    static Object read(String json) throws Exception {
        return reader.readTree(json);
    }

    static String write(Object value) throws Exception {
        return prettyWriter.writeValueAsString(value) + untyped;
    }
}
