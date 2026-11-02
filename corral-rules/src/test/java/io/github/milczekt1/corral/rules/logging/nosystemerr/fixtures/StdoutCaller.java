package io.github.milczekt1.corral.rules.logging.nosystemerr.fixtures;

/** MUST IGNORE: touches System.out only. */
public class StdoutCaller {

    public void report(int count) {
        System.out.println(count);
    }
}
