package io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures;

import java.time.Duration;

/**
 * MUST IGNORE: calls domain methods named {@code notify}, {@code notifyAll}, {@code wait} and
 * {@code timedWait}, so a predicate dropping the parameter list or the {@code TimeUnit} owner fails
 * here.
 */
public class AlertService {

    private final Notifier notifier;

    public AlertService(Notifier notifier) {
        this.notifier = notifier;
    }

    public void alert(long userId, long groupId) {
        notifier.wait(Duration.ofSeconds(1));
        notifier.timedWait(Duration.ofSeconds(1));
        notifier.notify(userId);
        notifier.notifyAll(groupId);
    }
}
