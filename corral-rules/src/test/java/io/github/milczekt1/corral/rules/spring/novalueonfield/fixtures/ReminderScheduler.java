package io.github.milczekt1.corral.rules.spring.novalueonfield.fixtures;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** MUST FLAG: a SpEL expression is still a field injected after construction. */
@Component
public class ReminderScheduler {

    @Value("#{T(java.time.Duration).ofMinutes(5)}")
    private Duration interval;

    public long intervalSeconds() {
        return interval.toSeconds();
    }
}
