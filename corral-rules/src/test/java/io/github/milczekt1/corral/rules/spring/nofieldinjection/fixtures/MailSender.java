package io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures;

import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

/** MUST FLAG: {@code jakarta.inject.Inject} on a package-private field. */
@Component
public class MailSender {

    @Inject
    OrderRepository outbox;

    public int queued() {
        return outbox.count();
    }
}
