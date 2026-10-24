package io.github.milczekt1.corral.rules.java.nofinalizeoverride.fixtures;

/**
 * MUST FLAG {@code finalize()}: an override releasing the resource. MUST IGNORE
 * {@code finalize(String)} and {@code close()}, so a predicate dropping the parameter or the name
 * check fails here.
 */
@SuppressWarnings("deprecation")
public class LeakyConnection {

    private boolean open = true;
    private String closeReason;

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    public void finalize(String reason) {
        closeReason = reason;
        close();
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public String closeReason() {
        return closeReason;
    }
}
