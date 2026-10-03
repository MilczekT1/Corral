package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

import java.io.Serializable;

/** MUST FLAG: the superclass's uid does not cover it; the JVM computes one for this class. */
public class ExpressPayment extends PaymentDto implements Serializable {

    private boolean priority;

    public boolean priority() {
        return priority;
    }
}
