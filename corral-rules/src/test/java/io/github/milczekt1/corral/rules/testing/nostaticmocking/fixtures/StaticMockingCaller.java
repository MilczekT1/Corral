package io.github.milczekt1.corral.rules.testing.nostaticmocking.fixtures;

import java.nio.file.Files;
import java.nio.file.Path;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * MUST FLAG on the {@code mockStatic} call. The {@code Mockito.mock} beside it is the must-not-match
 * half: an over-broad predicate finds that too.
 */
public class StaticMockingCaller {

    public void readsTheFile() {
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
            files.when(() -> Files.exists(Path.of("invoice.pdf"))).thenReturn(true);
        }
    }

    public Runnable ordinaryMock() {
        return Mockito.mock(Runnable.class);
    }
}
