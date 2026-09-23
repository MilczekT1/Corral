package io.github.milczekt1.corral.rules.testing.noconstructionmocking.fixtures;

import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * MUST FLAG on the {@code mockConstruction} call. The {@code Mockito.mock} beside it is the
 * must-not-match half: an over-broad predicate finds that too.
 */
public class ConstructionMockingCaller {

    public void buildsTheClient() {
        try (MockedConstruction<StringBuilder> clients = Mockito.mockConstruction(StringBuilder.class)) {
            clients.constructed();
        }
    }

    public Runnable ordinaryMock() {
        return Mockito.mock(Runnable.class);
    }
}
