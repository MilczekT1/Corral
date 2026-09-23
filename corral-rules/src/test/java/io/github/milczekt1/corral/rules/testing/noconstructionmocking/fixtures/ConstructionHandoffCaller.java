package io.github.milczekt1.corral.rules.testing.noconstructionmocking.fixtures;

import org.mockito.Mockito;

/** MUST FLAG on the call alone: the erased sink leaves this class depending on no handle type. */
public class ConstructionHandoffCaller {

    private final StringBuilder installed = new StringBuilder();

    public void install() {
        keep(Mockito.mockConstruction(StringBuilder.class));
    }

    public void keep(Object handle) {
        installed.append(handle);
    }
}
