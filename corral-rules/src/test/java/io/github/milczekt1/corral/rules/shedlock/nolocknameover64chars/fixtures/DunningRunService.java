package io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures;

import net.javacrumbs.shedlock.cdi.SchedulerLock;

/** MUST IGNORE: 65 characters, but ShedLock's CDI integration reads the name only from a method. */
@SchedulerLock(name = "com.acme.billing.invoicing.DunningRunService.closeStaleDunningRun")
public class DunningRunService {

    private int runs;

    public void closeStaleDunningRuns() {
        runs++;
    }
}
