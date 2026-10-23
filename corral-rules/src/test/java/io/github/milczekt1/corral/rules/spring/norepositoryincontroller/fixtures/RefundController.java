package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import org.springframework.web.bind.annotation.RestController;

/** MUST IGNORE: only its unannotated base class uses a repository, and the rule reads only the controller's own code. */
@RestController
public class RefundController extends AbstractRefundEndpoint {

    public long pending() {
        return orderCount();
    }
}
