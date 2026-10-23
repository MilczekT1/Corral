package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import javax.transaction.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG: {@code javax.transaction.Transactional}, honoured by Spring 5. */
@RestController
public class BillingController {

    @Transactional
    @PostMapping("/bills")
    public long bill() {
        return System.nanoTime();
    }
}
