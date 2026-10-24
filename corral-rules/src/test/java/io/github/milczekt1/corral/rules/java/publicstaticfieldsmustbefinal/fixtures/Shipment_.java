package io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal.fixtures;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/** MUST IGNORE: a jakarta static metamodel, whose attributes the specification requires to be {@code public static volatile}. */
@StaticMetamodel(ShipmentRecord.class)
public abstract class Shipment_ {

    public static final String ID = "id";

    public static volatile SingularAttribute<ShipmentRecord, Long> id;
}
