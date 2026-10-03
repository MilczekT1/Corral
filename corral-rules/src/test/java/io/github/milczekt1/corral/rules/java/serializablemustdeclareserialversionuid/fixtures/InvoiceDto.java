package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

import java.io.Serializable;

/** MUST IGNORE: a {@code public} uid is honoured by the JVM; access level is not checked. */
public class InvoiceDto implements Serializable {

    public static final long serialVersionUID = 1L;

    private String number;

    public String number() {
        return number;
    }
}
