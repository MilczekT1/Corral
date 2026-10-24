package io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Autowired;

/** MUST IGNORE: a {@code static final} mapper, an injected instance field and a non-final instance field with a setter. */
public class PaymentService {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private ObjectMapper injectedMapper;

    private ObjectMapper configuredMapper;

    void setConfiguredMapper(ObjectMapper configuredMapper) {
        this.configuredMapper = configuredMapper;
    }

    String describe(Object payment) throws Exception {
        return MAPPER.writeValueAsString(payment)
                + injectedMapper.writeValueAsString(payment)
                + configuredMapper.writeValueAsString(payment);
    }
}
