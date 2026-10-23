package io.github.milczekt1.corral.rules.concurrency.noobjectmonitormethods.fixtures;

/** MUST FLAG the timed {@code wait} overloads and {@code notify}, each called on {@code this}. */
public class RetryPacer {

    private boolean released;

    public synchronized void pause() throws InterruptedException {
        wait(1000);
    }

    public synchronized void pauseBriefly() throws InterruptedException {
        wait(0, 500);
    }

    public synchronized void release() {
        released = true;
        notify();
    }

    public synchronized boolean isReleased() {
        return released;
    }
}
