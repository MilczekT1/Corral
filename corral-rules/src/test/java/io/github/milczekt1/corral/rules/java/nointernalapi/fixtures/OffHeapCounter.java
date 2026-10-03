package io.github.milczekt1.corral.rules.java.nointernalapi.fixtures;

import java.util.concurrent.atomic.AtomicLong;
import sun.misc.Unsafe;

/** MUST FLAG the {@code Unsafe} field only: the {@code AtomicLong} beside it is supported API. */
public class OffHeapCounter {

    private Unsafe unsafe;

    private final AtomicLong fallback = new AtomicLong();

    public long increment() {
        return fallback.incrementAndGet();
    }
}
