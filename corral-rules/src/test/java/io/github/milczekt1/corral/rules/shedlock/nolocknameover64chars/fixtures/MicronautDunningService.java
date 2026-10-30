package io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars.fixtures;

import net.javacrumbs.shedlock.micronaut.SchedulerLock;

/** MUST FLAG: a 65-character name on the class, which locks every method. */
@SchedulerLock(name = "com.acme.billing.invoicing.MicronautDunningService.closeStaleRuns")
public class MicronautDunningService {

    private int runs;

    public void closeStaleDunningRuns() {
        runs++;
    }
}
