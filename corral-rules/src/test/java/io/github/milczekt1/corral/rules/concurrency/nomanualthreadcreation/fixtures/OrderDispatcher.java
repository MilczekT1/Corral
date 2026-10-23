package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures;

/**
 * MUST FLAG both {@code Thread} constructor calls. MUST IGNORE {@code Thread.currentThread()}, a
 * method on {@code Thread}, and {@code new StringBuilder()}, a constructor of another type.
 */
public class OrderDispatcher {

    public void placeAsync(Runnable submission) {
        new Thread(submission).start();
    }

    public void placeNamed(Runnable submission) {
        new Thread(submission, "worker").start();
    }

    public String describeCaller() {
        return new StringBuilder("placed by ").append(Thread.currentThread().getName()).toString();
    }
}
