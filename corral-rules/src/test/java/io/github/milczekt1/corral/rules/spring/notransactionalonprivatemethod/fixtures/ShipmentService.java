package io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures;

import org.springframework.transaction.annotation.Transactional;

/** MUST FLAG: {@code private final} is still private. */
public class ShipmentService {

    private int dispatched;

    public void ship() {
        dispatchParcel();
    }

    @Transactional
    private final void dispatchParcel() {
        dispatched++;
    }

    public int dispatched() {
        return dispatched;
    }
}
