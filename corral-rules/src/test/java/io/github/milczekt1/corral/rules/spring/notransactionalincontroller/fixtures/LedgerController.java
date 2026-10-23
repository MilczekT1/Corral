package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

/** MUST FLAG: a {@code @Controller} annotated {@code @Transactional} at class level. */
@Controller
@Transactional
public class LedgerController {

    @GetMapping("/ledger")
    public String ledger() {
        return "ledger";
    }
}
