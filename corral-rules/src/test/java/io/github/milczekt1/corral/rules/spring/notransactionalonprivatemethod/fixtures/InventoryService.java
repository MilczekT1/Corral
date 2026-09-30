package io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures;

import jakarta.transaction.Transactional;

/** MUST FLAG: {@code jakarta.transaction.Transactional} on a private method. */
public class InventoryService {

    private int reserved;

    public void reserve() {
        reserveStock();
    }

    @Transactional
    private void reserveStock() {
        reserved++;
    }

    public int reserved() {
        return reserved;
    }
}
