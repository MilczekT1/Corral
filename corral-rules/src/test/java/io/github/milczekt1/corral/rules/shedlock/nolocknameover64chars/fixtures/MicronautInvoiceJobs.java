package io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures;

import net.javacrumbs.shedlock.micronaut.SchedulerLock;

public class MicronautInvoiceJobs {

    private int runs;

    /** MUST FLAG: 69 characters on the Micronaut annotation. */
    @SchedulerLock(name = "com.acme.billing.invoicing.MicronautReminderJobs.sendOverdueReminders")
    public void sendOverdueReminders() {
        runs++;
    }
}
