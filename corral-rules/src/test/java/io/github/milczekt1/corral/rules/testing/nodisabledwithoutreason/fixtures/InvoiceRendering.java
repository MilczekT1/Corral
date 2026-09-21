package io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures;

import org.junit.jupiter.api.Test;

/** MUST NOT FLAG: a <em>use</em> of the composed annotation, which is the declared non-goal. */
public class InvoiceRendering {

    @PendingFix
    @Test
    void rendersTheInvoice() {
    }
}
