package io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures;

import java.time.Duration;

/** MUST IGNORE: declares, but never calls, methods named like the monitor methods with other parameter lists. */
public interface Notifier {

    void notify(long userId);

    void notifyAll(long groupId);

    void wait(Duration backoff);

    void timedWait(Duration backoff);
}
