package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

import java.io.Serializable;

/** MUST FLAG: no {@code serialVersionUID}; the {@code static final long} beside it has another name. */
public class OrderDto implements Serializable {

    private static final long SCHEMA_VERSION = 2L;

    private String reference;

    public String describe() {
        return reference + " v" + SCHEMA_VERSION;
    }
}
