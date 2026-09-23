package io.github.milczekt1.corral.rules.testing.noconstructionmocking.fixtures;

import org.mockito.MockedConstruction;

/** MUST FLAG on the parameter type: the helper the installer was moved into. */
public class ConstructionHandleReceiver {

    private MockedConstruction<StringBuilder> openClients;

    public void register(MockedConstruction<StringBuilder> clients) {
        openClients = clients;
    }
}
