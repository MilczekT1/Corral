package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import io.mockk.MockKException;
import java.util.concurrent.Callable;

/** MUST FLAG {@code io.mockk..}: MockK's exception type thrown from production code. */
public class SchedulerBridge {

    <T> T run(Callable<T> job) {
        try {
            return job.call();
        } catch (Exception e) {
            throw new MockKException("job failed", e);
        }
    }
}
