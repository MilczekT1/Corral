package io.github.milczekt1.corral.rules.spring.noapplicationcontextinjection.fixtures;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

/** MUST FLAG: a subtype of {@code ApplicationContext} is still the container. */
@Service
public class InvoiceService {

    private final ConfigurableApplicationContext context;

    public InvoiceService(ConfigurableApplicationContext context) {
        this.context = context;
    }

    public String issuer() {
        return context.getId();
    }
}
