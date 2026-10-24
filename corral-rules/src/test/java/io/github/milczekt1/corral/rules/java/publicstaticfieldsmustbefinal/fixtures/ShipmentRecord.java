package io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST IGNORE: the jakarta entity {@link Shipment_} describes, with no public static field. */
@Entity
public class ShipmentRecord {

    @Id
    private Long id;

    public Long getId() {
        return id;
    }
}
