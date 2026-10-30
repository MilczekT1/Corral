package io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

public class SpringInvoiceJobs {

    private static final String DUNNING_LOCK = "com.acme.billing.invoicing.InvoiceReminderJobs.closeStaleDunningRuns";

    private int runs;

    /** MUST FLAG: 72 characters, the fully-qualified class plus method. */
    @Scheduled(cron = "0 0 8 * * *")
    @SchedulerLock(name = "com.acme.billing.invoicing.InvoiceReminderScheduler.sendOverdueReminders")
    public void sendOverdueReminders() {
        runs++;
    }

    /** MUST FLAG: a 68-character constant, inlined into the annotation by javac. */
    @SchedulerLock(name = DUNNING_LOCK)
    public void closeStaleDunningRuns() {
        runs++;
    }

    /** MUST FLAG: 65 characters, one over the limit. */
    @SchedulerLock(name = "billing.InvoiceReminderScheduler.escalateUnpaidInvoicesToAgencies")
    public void escalateUnpaidInvoices() {
        runs++;
    }

    /** MUST IGNORE: exactly 64 characters. */
    @SchedulerLock(name = "billing.InvoiceReminderScheduler.sendPaymentConfirmationsToBuyer")
    public void sendPaymentConfirmations() {
        runs++;
    }

    /** MUST IGNORE: 64 characters, 21 of them outside the BMP, so 85 UTF-16 units. */
    @SchedulerLock(name = "billing.collection-team-notifications.𝐨𝐯𝐞𝐫𝐝𝐮𝐞-𝐫𝐞𝐦𝐢𝐧𝐝𝐞𝐫𝐬-𝐝𝐚𝐢𝐥𝐲.v2")
    public void notifyCollectionsTeam() {
        runs++;
    }

    /** MUST IGNORE: a 73-character placeholder, resolved at runtime. */
    @SchedulerLock(name = "${shedlock.names.invoice-reminders:billing.invoice-overdue-reminders-job}")
    public void remindersFromConfig() {
        runs++;
    }

    /** MUST IGNORE: a SpEL template longer than 64, resolved at runtime. */
    @SchedulerLock(name = "#{'billing.overdue-reminders.' + #tenant.substring(0, T(Math).min(#tenant.length(), 16))}")
    public void remindersForTenant(String tenant) {
        runs += tenant.length();
    }

    /** MUST IGNORE: the 72-character string is lockAtMostFor, not the name. */
    @SchedulerLock(name = "billing.purge-expired-invoices",
            lockAtMostFor = "PT30M, held at most thirty minutes while the purge deletes expired rows.")
    public void purgeExpiredInvoices() {
        runs++;
    }

    /** MUST IGNORE: the 76-character string is the cron expression, not the lock name. */
    @Scheduled(cron = "0 0,15,30,45 0-5,22-23 1-7,15-21 JAN,MAR,MAY,JUL,SEP,NOV MON,TUE,WED,THU,FRI")
    @SchedulerLock(name = "billing.archive-settled-invoices")
    public void archiveSettledInvoices() {
        runs++;
    }

    /** MUST IGNORE: the over-long name is reported once, on the composed annotation type. */
    @OverdueRemindersLock
    public void sendRemindersThroughComposedLock() {
        runs++;
    }
}
