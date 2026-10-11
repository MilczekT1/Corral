package io.github.milczekt1.corral.rules.java.nooptionalmethodparameters.fixtures;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * MUST IGNORE: javac compiles the lambda to a synthetic method taking {@code Optional}, and passes the
 * captured local to the anonymous class's constructor.
 */
public class ReminderTask {

    public List<String> recipients(Stream<Optional<String>> candidates) {
        return candidates.map(candidate -> candidate.orElse("nobody")).toList();
    }

    public Runnable deferred(String recipient, Consumer<String> send) {
        Optional<String> trimmed = Optional.ofNullable(recipient).map(String::trim);
        return new Runnable() {
            @Override
            public void run() {
                trimmed.ifPresent(send);
            }
        };
    }
}
