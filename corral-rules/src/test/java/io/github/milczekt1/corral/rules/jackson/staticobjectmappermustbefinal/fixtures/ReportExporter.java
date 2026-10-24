package io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.fixtures;

import com.fasterxml.jackson.databind.json.JsonMapper;

/** MUST FLAG {@code jsonMapper}: a Jackson 2 {@code JsonMapper} is assignable to {@code ObjectMapper}. */
public class ReportExporter {

    static JsonMapper jsonMapper = JsonMapper.builder().build();

    static byte[] export(Object report) throws Exception {
        return jsonMapper.writeValueAsBytes(report);
    }
}
