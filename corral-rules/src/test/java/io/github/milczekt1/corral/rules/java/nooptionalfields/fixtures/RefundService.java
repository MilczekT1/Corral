package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** MUST IGNORE: an optional dependency of a {@code @Service}, injected through the constructor. */
@Service
public class RefundService {

    private final Optional<Clock> auditClock;

    public RefundService(Optional<Clock> auditClock) {
        this.auditClock = auditClock;
    }

    public boolean audits() {
        return auditClock.isPresent();
    }
}
