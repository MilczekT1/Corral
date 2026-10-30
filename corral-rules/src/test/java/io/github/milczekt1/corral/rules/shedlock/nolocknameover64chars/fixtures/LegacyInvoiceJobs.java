package io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures;

import net.javacrumbs.shedlock.core.SchedulerLock;

public class LegacyInvoiceJobs {

    private int runs;

    /** MUST FLAG: 71 characters on ShedLock 4.x's core annotation. */
    @SchedulerLock(name = "com.acme.billing.invoicing.LegacyReminderScheduler.sendOverdueReminders")
    public void sendOverdueReminders() {
        runs++;
    }
}
