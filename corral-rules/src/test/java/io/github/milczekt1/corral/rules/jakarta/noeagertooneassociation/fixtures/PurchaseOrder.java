package io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.List;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/** Jakarta Persistence, field access, with Hibernate fetch modes. */
@Entity
public class PurchaseOrder {

    @Id
    private Long id;

    /** MUST FLAG: no {@code fetch}, so the specification default {@code EAGER} applies. */
    @ManyToOne
    private Customer customer;

    /** MUST FLAG: {@code optional} is not {@code fetch}. */
    @ManyToOne(optional = false)
    private Customer approver;

    /** MUST FLAG: declared {@code EAGER}. */
    @OneToOne(fetch = FetchType.EAGER)
    private Address billingAddress;

    /** MUST FLAG: {@code JOIN} makes Hibernate load it eagerly whatever {@code fetch} says. */
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    private Warehouse warehouse;

    /** MUST FLAG once, though both the default {@code fetch} and {@code JOIN} make it eager. */
    @ManyToOne
    @Fetch(FetchMode.JOIN)
    private Merchant reseller;

    /** MUST IGNORE: declared {@code LAZY}. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Merchant courier;

    /** MUST IGNORE: {@code SELECT} keeps a {@code LAZY} to-one lazy. */
    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    private Merchant merchant;

    /** MUST IGNORE: a to-many association, {@code LAZY} by default. */
    @OneToMany
    private List<Address> deliveryAddresses;

    public Long id() {
        return id;
    }
}
