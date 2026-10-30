package io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/** MUST FLAG: a composed annotation fixing a 72-character name. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@SchedulerLock(name = "com.acme.billing.invoicing.InvoiceReminderScheduler.sendOverdueReminders")
public @interface OverdueRemindersLock {
}
