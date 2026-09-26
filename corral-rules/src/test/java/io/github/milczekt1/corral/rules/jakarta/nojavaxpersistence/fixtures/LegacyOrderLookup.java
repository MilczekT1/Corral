package io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures;

import javax.persistence.EntityManager;
import javax.persistence.Query;

/** MUST FLAG: an {@code EntityManager} field, and a method returning a {@code javax} {@code Query}. */
public class LegacyOrderLookup {

    private final EntityManager entityManager;

    public LegacyOrderLookup(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Query byCustomer(String customer) {
        return entityManager.createQuery("select o from Order o where o.customer = :customer")
                .setParameter("customer", customer);
    }
}
