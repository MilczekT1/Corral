package io.github.milczekt1.corral.rules.jakarta.noeagercollection.fixtures;

import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

/** {@code javax} Persistence, field access. */
@Entity
public class LegacyCart {

    @Id
    private Long id;

    /** MUST FLAG: a {@code javax} one-to-many declared {@code EAGER}. */
    @OneToMany(fetch = FetchType.EAGER)
    private Set<Product> items;

    /** MUST FLAG: a {@code javax} many-to-many declared {@code EAGER}. */
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Product> wishlist;

    /** MUST FLAG: a {@code javax} element collection declared {@code EAGER}. */
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> labels;

    /** MUST IGNORE: a {@code javax} one-to-many with no {@code fetch}. */
    @OneToMany
    private Set<Product> returns;

    /** MUST IGNORE: a {@code javax} element collection declared {@code LAZY}. */
    @ElementCollection(fetch = FetchType.LAZY)
    private Set<String> notes;

    public Long id() {
        return id;
    }
}
