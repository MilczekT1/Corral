package io.github.milczekt1.corral.rules.spring.nosetterinjection.fixtures;

import org.springframework.beans.factory.annotation.Autowired;

/** MUST FLAG: the annotation is matched, not a {@code set} prefix. */
public class ReportScheduler {

    private OrderRepository orders;
    private OrderRepository archive;

    @Autowired
    public void wire(OrderRepository orders, OrderRepository archive) {
        this.orders = orders;
        this.archive = archive;
    }

    public int total() {
        return orders.count() + archive.count();
    }
}
