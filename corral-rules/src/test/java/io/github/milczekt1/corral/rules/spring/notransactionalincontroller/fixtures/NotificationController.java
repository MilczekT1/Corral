package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.bind.annotation.RestController;

/** MUST IGNORE: {@code @TransactionalEventListener} is an event listener, not a transaction boundary. */
@RestController
public class NotificationController {

    private final List<Object> received = new ArrayList<>();

    @TransactionalEventListener
    public void on(Object event) {
        received.add(event);
    }
}
