package io.github.milczekt1.corral.rules.java.nointernalapi.fixtures;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/** MUST IGNORE: the supported replacement for {@code Unsafe} field access. */
public class VarHandleCounter {

    private static final VarHandle COUNT;

    private volatile long count;

    static {
        try {
            COUNT = MethodHandles.lookup().findVarHandle(VarHandleCounter.class, "count", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public long increment() {
        return (long) COUNT.getAndAdd(this, 1L) + 1L;
    }
}
