package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures;

import org.springframework.stereotype.Component;

/** MUST FLAG: an abstract {@code @Component}, which component scanning skips. */
@Component
public abstract class AbstractOrderHandler {

    public abstract boolean handle(String orderId);

    public String describe() {
        return getClass().getSimpleName();
    }
}
