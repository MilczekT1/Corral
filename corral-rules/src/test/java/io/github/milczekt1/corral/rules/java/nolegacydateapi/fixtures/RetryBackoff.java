package io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures;

import java.util.concurrent.TimeUnit;

/** MUST IGNORE: {@code TimeUnit} is not a {@code TimeZone}. */
public class RetryBackoff {

    public long delayMillis(long seconds) {
        return TimeUnit.SECONDS.toMillis(seconds);
    }
}
