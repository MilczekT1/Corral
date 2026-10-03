package io.github.milczekt1.corral.rules.java.serializablemustdeclareserialversionuid.fixtures;

import java.io.Serializable;

/** MUST IGNORE: an interface has no serialized form of its own. */
public interface Command extends Serializable {

    void execute();
}
