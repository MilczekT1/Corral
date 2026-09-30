package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures;

import jakarta.transaction.Transactional;

/** MUST FLAG: {@code jakarta.transaction.Transactional} on a final method. */
public class InventoryService {

    private int reserved;

    @Transactional
    public final void reserveStock() {
        reserved++;
    }

    public int reserved() {
        return reserved;
    }
}
