package io.github.milczekt1.corral.rules.testing.nostaticmocking.fixtures;

import java.nio.file.Files;
import org.mockito.MockedStatic;

/** MUST FLAG on the parameter type: the helper the installer was moved into. */
public class StaticHandleReceiver {

    private MockedStatic<Files> openFiles;

    public void register(MockedStatic<Files> files) {
        openFiles = files;
    }
}
