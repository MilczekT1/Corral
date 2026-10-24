package io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures;

/**
 * MUST FLAG {@code printStackTrace()} on a project exception, which is recorded with that exception as
 * the owner rather than {@code Throwable}.
 */
public class SettlementJob {

    public String settle(String orderId) {
        try {
            reconcile(orderId);
            return "settled";
        } catch (SettlementException failure) {
            failure.printStackTrace();
            return failure.printableSummary();
        }
    }

    private void reconcile(String orderId) {
        if (orderId.isBlank()) {
            throw new SettlementException("no order id");
        }
    }
}
