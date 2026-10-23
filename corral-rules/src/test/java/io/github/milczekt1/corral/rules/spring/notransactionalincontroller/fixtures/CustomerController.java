package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

/** MUST FLAG once, not twice: javac copies the annotation onto the synthetic bridge {@code save(Object)}. */
@RestController
public class CustomerController implements EntityWriter<String> {

    @Override
    @Transactional
    public String save(String customer) {
        return customer.strip();
    }
}
