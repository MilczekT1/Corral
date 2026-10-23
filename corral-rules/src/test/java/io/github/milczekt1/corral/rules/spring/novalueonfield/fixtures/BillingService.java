package io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** MUST IGNORE: the shape Lombok's {@code copyableAnnotations} generates — a final field and a constructor parameter, both annotated. */
@Service
public class BillingService {

    @Value("${app.timeout}")
    private final Duration timeout;

    public BillingService(@Value("${app.timeout}") Duration timeout) {
        this.timeout = timeout;
    }

    public Duration timeout() {
        return timeout;
    }
}
