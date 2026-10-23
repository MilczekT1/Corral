package io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/** MUST IGNORE: a {@code Condition} awaited in a loop and signalled, which this rule does not match. */
public class HandoffBuffer {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition filled = lock.newCondition();
    private String item;

    public String take() throws InterruptedException {
        lock.lock();
        try {
            while (item == null) {
                if (!filled.await(1, TimeUnit.SECONDS)) {
                    return null;
                }
            }
            String taken = item;
            item = null;
            return taken;
        } finally {
            lock.unlock();
        }
    }

    public void put(String value) {
        lock.lock();
        try {
            item = value;
            filled.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
