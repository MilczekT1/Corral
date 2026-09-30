package io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures;

import org.springframework.transaction.annotation.Transactional;

/** MUST IGNORE: a class-level {@code @Transactional} does not apply to its private methods. */
@Transactional
public class PaymentService {

    private int attempts;

    public void capture() {
        recordAttempt();
    }

    private void recordAttempt() {
        attempts++;
    }

    public int attempts() {
        return attempts;
    }
}
