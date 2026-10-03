package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

import java.io.Serializable;

/** MUST IGNORE: declares a {@code private static final long serialVersionUID}. */
public class PaymentDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reference;

    public String reference() {
        return reference;
    }
}
