package io.github.milczekt1.corral.rules.jackson.noannotationsonentity.fixtures;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST FLAG the constructor parameter: Jackson binds through it. */
@Entity
public class Ledger {

    @Id
    private Long id;

    protected Ledger() {
    }

    public Ledger(@JsonProperty("ledger_id") Long id) {
        this.id = id;
    }
}
