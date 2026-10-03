package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

import java.io.Serializable;

/** MUST FLAG: the field is not {@code static}, so the JVM ignores it. */
public class CartDto implements Serializable {

    private final long serialVersionUID = 1L;

    private int itemCount;

    public int itemCount() {
        return itemCount;
    }
}
