package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

import java.io.Serializable;

/** MUST FLAG: an abstract class's uid is part of every subclass's serialized form. */
public abstract class AbstractLineItem implements Serializable {

    public abstract long priceInCents();
}
