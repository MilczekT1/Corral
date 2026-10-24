package io.github.milczekt1.corral.rules.java.nofinalizeoverride.fixtures;

/** MUST IGNORE: deterministic cleanup, and a domain {@code finalize(String)} that only shares the name. */
public class SafeConnection implements AutoCloseable {

    private boolean open = true;
    private String settledOrder;

    @Override
    public void close() {
        open = false;
    }

    void finalize(String orderId) {
        settledOrder = orderId;
    }

    public boolean isOpen() {
        return open;
    }

    public String settledOrder() {
        return settledOrder;
    }
}
