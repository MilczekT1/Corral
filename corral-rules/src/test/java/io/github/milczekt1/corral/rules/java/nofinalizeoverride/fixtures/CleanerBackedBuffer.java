package io.github.milczekt1.corral.rules.java.nofinalizeoverride.fixtures;

import java.lang.ref.Cleaner;

/** MUST IGNORE: the {@code Cleaner} backstop, with an action that does not capture {@code this}. */
public class CleanerBackedBuffer implements AutoCloseable {

    private static final Cleaner CLEANER = Cleaner.create();

    private final Cleaner.Cleanable cleanable;

    public CleanerBackedBuffer(int size) {
        cleanable = CLEANER.register(this, new Release(new byte[size]));
    }

    @Override
    public void close() {
        cleanable.clean();
    }

    private static final class Release implements Runnable {

        private byte[] buffer;

        private Release(byte[] buffer) {
            this.buffer = buffer;
        }

        @Override
        public void run() {
            buffer = null;
        }
    }
}
