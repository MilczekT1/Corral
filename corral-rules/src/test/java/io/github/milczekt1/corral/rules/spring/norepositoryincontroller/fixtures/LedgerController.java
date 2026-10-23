package io.github.milczekt1.corral.rules.spring.norepositoryincontroller.fixtures;

import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG: a {@code @Repository}-annotated class with no Spring Data supertype. */
@RestController
public class LedgerController {

    private final LedgerDao ledger;

    public LedgerController(LedgerDao ledger) {
        this.ledger = ledger;
    }

    public void record(String entry) {
        ledger.append(entry);
    }
}
