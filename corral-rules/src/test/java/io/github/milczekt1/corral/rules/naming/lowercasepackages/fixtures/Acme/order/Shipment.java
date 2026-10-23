package io.github.milczekt1.corral.rules.naming.lowercasepackages.fixtures.Acme.order;

/** MUST FLAG: the uppercase is in a middle segment, not the last one. */
public class Shipment {

    public String label(String carrier, String trackingNumber) {
        return carrier + ":" + trackingNumber;
    }
}
