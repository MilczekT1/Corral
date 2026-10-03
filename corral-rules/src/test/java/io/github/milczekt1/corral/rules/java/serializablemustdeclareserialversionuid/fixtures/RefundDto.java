package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

import java.io.Serializable;

/** MUST FLAG: the JVM widens an {@code int} uid, but the serialization specification requires {@code long}. */
public class RefundDto implements Serializable {

    private static final int serialVersionUID = 1;

    private long amountInCents;

    public long amountInCents() {
        return amountInCents;
    }
}
