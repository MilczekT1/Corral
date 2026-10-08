package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures;

import org.springframework.stereotype.Service;

/** MUST IGNORE: a concrete {@code @Service}, extending a flagged base. */
@Service
public class CheckoutHandler extends AbstractOrderHandler {

    @Override
    public boolean handle(String orderId) {
        return !orderId.isEmpty();
    }
}
