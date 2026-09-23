package io.github.milczekt1.corral.rules.testing.nostaticmocking.fixtures;

import java.nio.file.Files;
import org.mockito.MockedStatic;

/** MUST FLAG on the field type: the shared-base-class shape, with no installer call of its own. */
public class StaticHandleHolder {

    MockedStatic<Files> openFiles;
}
