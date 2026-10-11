package io.github.milczekt1.corral.rules.java.nooptionalfields.fixtures;

import java.util.Optional;
import java.util.function.Consumer;

/** MUST IGNORE: javac stores a local captured by an anonymous class in a synthetic field; a lambda stores none. */
public class ReminderTask {

    public Runnable anonymous(Optional<String> recipient, Consumer<String> send) {
        Optional<String> trimmed = recipient.map(String::trim);
        return new Runnable() {
            @Override
            public void run() {
                trimmed.ifPresent(send);
            }
        };
    }

    public Runnable lambda(Optional<String> recipient, Consumer<String> send) {
        Optional<String> trimmed = recipient.map(String::trim);
        return () -> trimmed.ifPresent(send);
    }
}
