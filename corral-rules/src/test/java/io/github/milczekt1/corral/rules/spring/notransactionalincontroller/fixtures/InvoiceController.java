package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import jakarta.transaction.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG: {@code jakarta.transaction.Transactional} on a handler. */
@RestController
public class InvoiceController {

    @Transactional
    @PostMapping("/invoices")
    public long issue() {
        return System.nanoTime();
    }
}
