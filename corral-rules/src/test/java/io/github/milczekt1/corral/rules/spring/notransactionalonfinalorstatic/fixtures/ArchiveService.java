package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures;

import java.util.function.Function;
import org.springframework.transaction.annotation.Transactional;

/**
 * MUST FLAG {@code apply(String)} once: the compiler also emits a synthetic bridge
 * {@code apply(Object)}, which must not be reported a second time.
 */
@Transactional
public final class ArchiveService implements Function<String, String> {

    @Override
    public String apply(String document) {
        return "archived: " + document;
    }
}
