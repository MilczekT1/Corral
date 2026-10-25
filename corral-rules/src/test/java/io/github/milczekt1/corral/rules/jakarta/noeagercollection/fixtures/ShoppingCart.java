package io.github.milczekt1.corral.rules.jakarta.noeagercollection.fixtures;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.Set;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/** Jakarta Persistence, field access, with Hibernate fetch modes. */
@Entity
public class ShoppingCart {

    @Id
    private Long id;

    /** MUST FLAG: a one-to-many declared {@code EAGER}. */
    @OneToMany(fetch = FetchType.EAGER)
    private Set<Product> items;

    /** MUST FLAG: a many-to-many declared {@code EAGER}. */
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Product> coupons;

    /** MUST FLAG: an element collection declared {@code EAGER}. */
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> labels;

    /** MUST FLAG: {@code JOIN} makes Hibernate load it eagerly whatever {@code fetch} says. */
    @OneToMany(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    private Set<Product> shipments;

    /** MUST FLAG once, though both {@code EAGER} and {@code JOIN} make it eager. */
    @ManyToMany(fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    private Set<Product> vouchers;

    /** MUST IGNORE: no {@code fetch}, so the specification default {@code LAZY} applies. */
    @OneToMany
    private Set<Product> refunds;

    /** MUST IGNORE: an element collection with no {@code fetch}. */
    @ElementCollection
    private Set<String> notes;

    /** MUST IGNORE: declared {@code LAZY}. */
    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Product> tags;

    /** MUST IGNORE: {@code SUBSELECT} keeps a {@code LAZY} collection lazy. */
    @OneToMany(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    private Set<Product> allocations;

    /** MUST IGNORE: a to-one declared {@code EAGER} is out of scope. */
    @ManyToOne(fetch = FetchType.EAGER)
    private Shopper owner;

    /** MUST IGNORE: {@code JOIN} on a to-one is out of scope. */
    @ManyToOne
    @Fetch(FetchMode.JOIN)
    private Shopper referrer;

    public Long id() {
        return id;
    }
}
