package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG: its interface inherits a type-level {@code @Transactional} from a superinterface. */
@RestController
public class DigestController implements DigestOperations {

    @Override
    public String export(String format) {
        return "digest." + format;
    }
}
