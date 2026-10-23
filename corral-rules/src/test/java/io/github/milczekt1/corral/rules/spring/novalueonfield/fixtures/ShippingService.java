package io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures;

import java.time.Duration;
import org.springframework.stereotype.Service;

/** MUST FLAG: {@link AppTimeout} is meta-annotated {@code @Value}, so this is the same field injection. */
@Service
public class ShippingService {

    @AppTimeout
    private Duration timeout;

    public Duration timeout() {
        return timeout;
    }
}
