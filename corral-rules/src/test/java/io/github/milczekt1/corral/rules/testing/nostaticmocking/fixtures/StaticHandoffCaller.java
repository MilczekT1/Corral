package io.github.milczekt1.corral.rules.testing.nostaticmocking.fixtures;

import java.nio.file.Files;
import org.mockito.Mockito;

/** MUST FLAG on the call alone: the erased sink leaves this class depending on no handle type. */
public class StaticHandoffCaller {

    private final StringBuilder installed = new StringBuilder();

    public void install() {
        keep(Mockito.mockStatic(Files.class));
    }

    public void keep(Object handle) {
        installed.append(handle);
    }
}
