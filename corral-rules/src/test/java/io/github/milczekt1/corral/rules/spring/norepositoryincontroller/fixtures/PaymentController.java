package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.RestController;

/** MUST IGNORE: a service, and a Spring Security type that only shares the {@code Repository} suffix. */
@RestController
public class PaymentController {

    private final PaymentService payments;
    private final SecurityContextRepository securityContextRepository;

    public PaymentController(PaymentService payments, SecurityContextRepository securityContextRepository) {
        this.payments = payments;
        this.securityContextRepository = securityContextRepository;
    }

    public boolean paid(long orderId) {
        return payments.isPaid(orderId);
    }

    public SecurityContextRepository securityContextRepository() {
        return securityContextRepository;
    }
}
