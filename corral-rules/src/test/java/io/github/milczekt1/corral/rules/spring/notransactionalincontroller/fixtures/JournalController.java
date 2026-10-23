package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG: inherits a class-level {@code @Transactional} from two superclasses up. */
@RestController
public class JournalController extends AbstractJournalEndpoint {

    public String append(String entry) {
        return stamp(entry);
    }
}
