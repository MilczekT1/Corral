package io.github.milczekt1.corral.rules.testing.nostaticmocking.fixtures;

import org.mockito.Mockito;

/** MUST NOT FLAG: ordinary Mockito, which installs nothing. */
public class PlainMockitoUser {

    public Runnable stubbed() {
        Runnable runnable = Mockito.mock(Runnable.class);
        Mockito.when(runnable.toString()).thenReturn("stubbed");
        return runnable;
    }
}
