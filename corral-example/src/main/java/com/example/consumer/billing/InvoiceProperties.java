package com.example.consumer.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Pre-existing debt for {@code corral.spring.configuration-properties-must-use-project-prefix}. */
@ConfigurationProperties("app.invoice")
public class InvoiceProperties {

    private int paymentTermDays;
}
