package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.util.Optional;
import org.powermock.reflect.Whitebox;

/** MUST FLAG {@code org.powermock..}: PowerMock's reflection helper reaching into a private field. */
public class ConfigInspector {

    Optional<Object> peek(Object target, String field) {
        return Optional.ofNullable(Whitebox.getInternalState(target, field));
    }
}
