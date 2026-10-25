package io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation.fixtures;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

/** {@code javax} Persistence, field access. */
@Entity
public class LegacyInvoice {

    @Id
    private Long id;

    /** MUST FLAG: a {@code javax} many-to-one with no {@code fetch}. */
    @ManyToOne
    private Customer customer;

    /** MUST FLAG: a {@code javax} one-to-one with no {@code fetch}. */
    @OneToOne
    private Customer payer;

    /** MUST IGNORE: a {@code javax} many-to-one declared {@code LAZY}. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Merchant vendor;

    /** MUST IGNORE: a {@code javax} one-to-one declared {@code LAZY}. */
    @OneToOne(fetch = FetchType.LAZY)
    private Address shippingAddress;

    public Long id() {
        return id;
    }
}
