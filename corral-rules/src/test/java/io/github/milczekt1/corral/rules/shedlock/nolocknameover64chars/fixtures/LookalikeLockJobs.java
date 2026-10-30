package io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures;

import io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures.lookalike.SchedulerLock;

public class LookalikeLockJobs {

    private int runs;

    /** MUST IGNORE: 72 characters, but on a project's own annotation that shares ShedLock's simple name. */
    @SchedulerLock(name = "com.acme.billing.invoicing.InvoiceReminderScheduler.sendOverdueReminders")
    public void sendOverdueReminders() {
        runs++;
    }
}
