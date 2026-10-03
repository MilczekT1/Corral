package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

import java.io.Serializable;

/** MUST FLAG: the field is not {@code final}, so the JVM ignores it. */
public class ShipmentDto implements Serializable {

    private static long serialVersionUID = 1L;

    private String trackingNumber;

    public String trackingNumber() {
        return trackingNumber;
    }
}
