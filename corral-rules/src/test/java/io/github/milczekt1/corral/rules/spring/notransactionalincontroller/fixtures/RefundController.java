package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG: inherits a {@code @Transactional} method from its unannotated base class. */
@RestController
public class RefundController extends AbstractClaimEndpoint {

    public String refund() {
        return "refunded " + settle();
    }
}
