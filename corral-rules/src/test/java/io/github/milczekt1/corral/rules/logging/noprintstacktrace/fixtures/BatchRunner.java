package io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures;

import java.util.List;

/** MUST FLAG {@code printStackTrace()} inside a lambda body. */
public class BatchRunner {

    public void runAll(List<Runnable> jobs) {
        jobs.forEach(job -> {
            try {
                job.run();
            } catch (RuntimeException failure) {
                failure.printStackTrace();
            }
        });
    }
}
