package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG on its own line: shares {@link AbstractClaimEndpoint} with {@link RefundController}. */
@RestController
public class ReturnController extends AbstractClaimEndpoint {

    public String accept() {
        return "accepted " + settle();
    }
}
