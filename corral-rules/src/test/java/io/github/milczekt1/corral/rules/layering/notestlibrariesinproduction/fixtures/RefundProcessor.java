package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MUST IGNORE: an ordinary production library. */
public class RefundProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RefundProcessor.class);

    void refund(String paymentId) {
        LOG.info("refunding {}", paymentId);
    }
}
