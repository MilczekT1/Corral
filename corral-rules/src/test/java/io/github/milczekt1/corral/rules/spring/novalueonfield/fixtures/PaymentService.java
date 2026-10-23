package io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** MUST IGNORE: constructor injection, with {@code @Value} on the parameter only. */
@Service
public class PaymentService {

    private final Duration timeout;

    public PaymentService(@Value("${app.timeout}") Duration timeout) {
        this.timeout = timeout;
    }

    public Duration timeout() {
        return timeout;
    }
}
