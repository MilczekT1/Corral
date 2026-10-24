package io.github.milczekt1.corral.rules.logging.noprintstacktrace.fixtures;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * MUST FLAG the method reference {@code Throwable::printStackTrace}. MUST IGNORE
 * {@code Throwable::getMessage}: a reference on the same owner, so a predicate dropping the name fails
 * here.
 */
public class FailureListener {

    public List<String> onFailures(List<Throwable> failures) {
        Consumer<Throwable> print = Throwable::printStackTrace;
        Function<Throwable, String> describe = Throwable::getMessage;
        failures.forEach(print);
        return failures.stream().map(describe).toList();
    }
}
