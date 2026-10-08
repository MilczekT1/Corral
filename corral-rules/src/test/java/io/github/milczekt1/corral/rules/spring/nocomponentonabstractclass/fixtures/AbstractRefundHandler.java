package io.github.milczekt1.corral.rules.spring.nocomponentonabstractclass.fixtures;

import org.springframework.stereotype.Component;

/**
 * MUST FLAG: its {@code @Lookup} method is inherited, not declared, and Spring checks only the
 * scanned class's own methods, so it is still no scan candidate.
 */
@Component
public abstract class AbstractRefundHandler extends AbstractLookupSupport {

    public String refund() {
        return freshBuffer().append("refunded").toString();
    }
}
