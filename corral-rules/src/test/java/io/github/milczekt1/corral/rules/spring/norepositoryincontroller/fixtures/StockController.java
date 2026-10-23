package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG the typed {@code getBean} lookup; MUST IGNORE the {@code ApplicationContext} it goes through. */
@RestController
public class StockController {

    private final ApplicationContext context;

    public StockController(ApplicationContext context) {
        this.context = context;
    }

    public long count() {
        return context.getBean(OrderRepository.class).count();
    }
}
