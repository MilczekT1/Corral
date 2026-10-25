package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

/** MUST IGNORE: a DTO with no persistence annotation. */
public class OrderSummary {

    private final long total;

    public OrderSummary(long total) {
        this.total = total;
    }

    public long total() {
        return total;
    }
}
