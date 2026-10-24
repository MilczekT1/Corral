package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.util.Assert;

/** MUST IGNORE: the same checks as the flagged examples, from production APIs. */
public class PaymentService {

    void charge(String account, BigDecimal amount) {
        Objects.requireNonNull(account);
        Assert.isTrue(amount.signum() > 0, "amount must be positive");
    }
}
