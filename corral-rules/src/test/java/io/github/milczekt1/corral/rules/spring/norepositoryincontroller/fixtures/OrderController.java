package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG the {@code CrudRepository} subtype; MUST IGNORE the service beside it and Spring Data's {@code Pageable}. */
@RestController
public class OrderController {

    private final OrderRepository orders;
    private final PricingService pricing;

    public OrderController(OrderRepository orders, PricingService pricing) {
        this.orders = orders;
        this.pricing = pricing;
    }

    public Optional<OrderSummary> show(long id) {
        return orders.findById(id);
    }

    public long quote(long id) {
        return pricing.quote(id);
    }

    public List<Integer> page(Pageable pageable) {
        return List.of(pageable.getPageNumber());
    }
}
