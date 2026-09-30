package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures;

/**
 * MUST FLAG {@code dispatchParcel} only: transactional through {@link CarrierBase}'s class-level
 * annotation. {@code trackParcel} is overridable.
 */
public class ShipmentService extends CarrierBase {

    public final void dispatchParcel() {
        parcels++;
    }

    public int trackParcel() {
        return parcels;
    }
}
