package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures;

/** MUST FLAG the implicit constructor's {@code super()} call to {@code Thread}. */
public class PollingWorker extends Thread {

    @Override
    public void run() {
        interrupt();
    }
}
