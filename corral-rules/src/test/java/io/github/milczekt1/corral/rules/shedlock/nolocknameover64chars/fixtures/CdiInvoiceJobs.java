package io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures;

import net.javacrumbs.shedlock.cdi.SchedulerLock;

public class CdiInvoiceJobs {

    private int runs;

    /** MUST FLAG: 70 characters on the CDI annotation. */
    @SchedulerLock(name = "com.acme.billing.invoicing.CdiInvoiceReminderJobs.sendOverdueReminders")
    public void sendOverdueReminders() {
        runs++;
    }

    /** MUST FLAG: 75 characters; ShedLock's CDI integration stores the placeholder as written. */
    @SchedulerLock(name = "${shedlock.names.dunning-runs:com.acme.billing.invoicing.CdiDunningRunJobs}")
    public void closeStaleDunningRuns() {
        runs++;
    }
}
