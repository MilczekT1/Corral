package io.github.milczekt1.corral.rules.java.nolegacydateapi.fixtures;

import java.util.Calendar;

/** MUST FLAG: returns a {@code Calendar}. */
public class BillingCycle {

    public Calendar nextRun() {
        Calendar next = Calendar.getInstance();
        next.add(Calendar.MONTH, 1);
        return next;
    }
}
