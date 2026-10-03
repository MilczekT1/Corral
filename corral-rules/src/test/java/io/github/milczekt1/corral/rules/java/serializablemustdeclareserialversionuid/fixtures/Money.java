package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

import java.io.Serializable;

/** MUST IGNORE: a record serializes through its canonical constructor. */
public record Money(long amountInCents, CurrencyCode currency) implements Serializable {
}
