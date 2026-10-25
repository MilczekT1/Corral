package io.github.milczekt1.corral.rules.jakarta.noeagertooneassociation.fixtures;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

/** Property access: the mapping is on the getters, so MUST IGNORE the unannotated fields. */
@Entity
@Access(AccessType.PROPERTY)
public class Subscription {

    private Long id;

    private Customer customer;

    private Address address;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** MUST FLAG: declared {@code EAGER} on a getter. */
    @ManyToOne(fetch = FetchType.EAGER)
    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    /** MUST IGNORE: declared {@code LAZY} on a getter. */
    @OneToOne(fetch = FetchType.LAZY)
    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
