package io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** MUST FLAG on both: an explicit empty reason and a whitespace-only one are each bare. */
public class ShipmentTracking {

    @Disabled("")
    @Test
    void tracksTheParcel() {
    }

    @Disabled("   ")
    @Test
    void notifiesTheCustomer() {
    }
}
