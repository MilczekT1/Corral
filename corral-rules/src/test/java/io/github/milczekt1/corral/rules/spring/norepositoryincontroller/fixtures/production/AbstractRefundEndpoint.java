package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures.production;

import org.springframework.beans.factory.annotation.Autowired;

/** SUPPORT: an unannotated base class, the only one that touches the repository. */
public abstract class AbstractRefundEndpoint {

    @Autowired
    private OrderRepository orders;

    protected long orderCount() {
        return orders.count();
    }
}
