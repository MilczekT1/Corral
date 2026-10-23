package io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * MUST IGNORE where it compiled: test output is out of scope. Its test copies it into a production
 * layout to show the same {@code get()} call is flagged there.
 */
public class AsyncAssertions {

    public static <T> T awaited(Future<T> result) throws InterruptedException, ExecutionException {
        return result.get();
    }
}
