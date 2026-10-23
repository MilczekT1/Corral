package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import org.springframework.stereotype.Service;

/** MUST IGNORE: a service, which is where a repository belongs. */
@Service
public class PaymentService {

    private final OrderRepository orders;

    public PaymentService(OrderRepository orders) {
        this.orders = orders;
    }

    public boolean isPaid(long orderId) {
        return orders.findById(orderId).map(order -> "PAID".equals(order.status())).orElse(false);
    }
}
