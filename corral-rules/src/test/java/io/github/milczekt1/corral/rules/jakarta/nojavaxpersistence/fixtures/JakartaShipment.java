package io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST IGNORE: the migrated namespace. */
@Entity
public class JakartaShipment {

    @Id
    private Long id;

    @Column(nullable = false)
    private String carrier;

    public String carrier() {
        return carrier;
    }
}
