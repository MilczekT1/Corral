package io.github.milczekt1.corral.rules.concurrency.nomanualthreadcreation.fixtures;

/**
 * MUST IGNORE where it compiled: test output is out of scope. Its test copies it into a production
 * layout to show the same constructor call is flagged there.
 */
public class RaceProvoker {

    public static Thread contender(Runnable race) {
        return new Thread(race);
    }
}
