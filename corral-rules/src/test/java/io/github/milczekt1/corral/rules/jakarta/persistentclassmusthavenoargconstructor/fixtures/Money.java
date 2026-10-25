package io.github.milczekt1.corral.rules.jakarta.persistentclassmusthavenoargconstructor.fixtures;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** MUST FLAG: a value object with only its invariant-enforcing constructor. */
@Embeddable
public class Money {

    private BigDecimal amount;
    private String currency;

    public Money(BigDecimal amount, String currency) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("negative amount");
        }
        this.amount = amount;
        this.currency = currency;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }
}
