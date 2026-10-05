package io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures;

/** MUST FLAG: a {@code java.sql.Date} field, matched as a {@code java.util.Date} subtype. */
public class PayrollRecord {

    private java.sql.Date paidOn;

    public boolean isPaid() {
        return paidOn != null;
    }
}
