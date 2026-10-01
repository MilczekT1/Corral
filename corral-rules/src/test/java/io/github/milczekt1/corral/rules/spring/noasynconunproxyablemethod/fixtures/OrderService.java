package io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures;

import org.springframework.scheduling.annotation.Async;

/**
 * MUST FLAG {@code notifyWarehouse}, {@code notifyCourier}, {@code notifyAudit}, {@code notifyTax},
 * {@code notifyBilling} and {@code purgeOutbox}: none can be overridden. {@code publishOrder} is
 * overridable and {@code listOrders} is not async.
 */
public class OrderService {

    private int notifications;

    @Async
    private void notifyWarehouse() {
        notifications++;
    }

    @Async
    public final void notifyCourier() {
        notifyWarehouse();
    }

    @Async
    static void notifyAudit() {
        purgeOutbox();
    }

    @Async
    public static final void notifyTax() {
        notifyAudit();
    }

    @Async
    private final void notifyBilling() {
        notifications += 2;
    }

    @Async
    private static void purgeOutbox() {
        Thread.yield();
    }

    @Async
    public void publishOrder() {
        notifyBilling();
    }

    public int listOrders() {
        return notifications;
    }
}
