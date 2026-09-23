package io.github.milczekt1.corral.rules.testing.noconstructionmocking.fixtures;

import org.mockito.MockedConstruction;

/** MUST FLAG on the field type: the shared-base-class shape, with no installer call of its own. */
public class ConstructionHandleHolder {

    MockedConstruction<StringBuilder> openClients;
}
