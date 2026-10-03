package io.github.milczekt1.corral.rules.java.nointernalapi.fixtures;

import sun.misc.Signal;

/** MUST FLAG: a {@code sun.misc.Signal} parameter, and the call on it. */
public class ReloadSignalHandler {

    public String handle(Signal signal) {
        return signal.getName();
    }
}
