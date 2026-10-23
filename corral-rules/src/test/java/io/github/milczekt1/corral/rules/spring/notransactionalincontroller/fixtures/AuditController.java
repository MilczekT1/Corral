package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG: inherits a class-level {@code @Transactional} from its base class. */
@RestController
public class AuditController extends TransactionalAuditBase {

    public String record(String entry) {
        return stamp(entry);
    }
}
