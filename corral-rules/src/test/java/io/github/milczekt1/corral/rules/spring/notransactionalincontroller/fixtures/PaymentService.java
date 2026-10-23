package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** MUST IGNORE: a {@code @Service} annotated {@code @Transactional} is where the boundary belongs. */
@Service
@Transactional
public class PaymentService {

    private long total;

    public long charge(long amount) {
        total += amount;
        return total;
    }
}
