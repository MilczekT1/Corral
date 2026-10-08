package io.github.milczekt1.corral.rules.spring.nofinalcomponent.fixtures;

import org.springframework.stereotype.Service;

/** MUST IGNORE: a bean the proxy can subclass; a final method is out of this rule's reach. */
@Service
public class PaymentService {

    public final int settle() {
        return 1;
    }
}
