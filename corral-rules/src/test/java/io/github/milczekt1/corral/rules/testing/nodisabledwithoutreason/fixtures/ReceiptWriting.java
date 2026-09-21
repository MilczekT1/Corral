package io.github.milczekt1.corral.rules.testing.nodisabledwithoutreason.fixtures;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** MUST FLAG on the type: the class-level branch, with nothing on the method. */
@Disabled
public class ReceiptWriting {

    @Test
    void writesTheReceipt() {
    }
}
