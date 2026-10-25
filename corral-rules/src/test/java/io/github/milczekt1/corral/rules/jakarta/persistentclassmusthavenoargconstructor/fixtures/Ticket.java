package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Instantiator;

/** MUST FLAG: Hibernate honours {@code @Instantiator} on embeddables only, not on entities. */
@Entity
public class Ticket {

    @Id
    private Long id;

    @Instantiator("id")
    public Ticket(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }
}
