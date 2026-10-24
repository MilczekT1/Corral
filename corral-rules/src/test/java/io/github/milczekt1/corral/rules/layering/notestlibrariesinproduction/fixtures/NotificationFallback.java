package io.github.milczekt1.corral.rules.layering.notestlibrariesinproduction.fixtures;

import java.util.function.Consumer;
import org.mockito.Mockito;

/** MUST FLAG {@code org.mockito..}: a Mockito mock standing in as a null object. */
public class NotificationFallback {

    @SuppressWarnings("unchecked")
    Consumer<String> silentSender() {
        return Mockito.mock(Consumer.class);
    }
}
