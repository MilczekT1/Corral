package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

import java.io.Serializable;

/** MUST IGNORE: the JVM ignores {@code serialVersionUID} on an enum. */
public enum CurrencyCode implements Serializable {
    EUR,
    PLN
}
