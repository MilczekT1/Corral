package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.EmbeddableInstantiator;

/** MUST FLAG: Hibernate applies {@code @EmbeddableInstantiator} to embeddables only, not to entities. */
@Entity
@EmbeddableInstantiator(CelsiusReader.class)
public class GiftCard {

    @Id
    private Long id;

    public GiftCard(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }
}
