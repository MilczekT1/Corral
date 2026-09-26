package io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** MUST FLAG the {@code javax} column only: the {@code jakarta} annotations beside it must not be reported. */
@Entity
public class HalfMigratedInvoice {

    @Id
    private Long id;

    @javax.persistence.Column(nullable = false)
    private String number;

    public String number() {
        return number;
    }
}
