package com.example.consumer.billing;

/** Pre-existing debt for this project's own {@code acme.services-must-live-in-service-package}. */
public class InvoiceService {

    public long totalInCents(long netInCents, long taxInCents) {
        return netInCents + taxInCents;
    }
}
