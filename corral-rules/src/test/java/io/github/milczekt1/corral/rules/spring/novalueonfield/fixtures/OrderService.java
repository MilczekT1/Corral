package io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** MUST FLAG {@code timeout}, set only after construction; MUST IGNORE {@code grace}, an unannotated field of the same type. */
@Service
public class OrderService {

    @Value("${app.timeout}")
    private Duration timeout;

    private Duration grace = Duration.ZERO;

    public Duration deadline() {
        return timeout.plus(grace);
    }
}
