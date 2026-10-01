package io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures;

import java.util.function.Function;
import org.springframework.scheduling.annotation.Async;

/**
 * MUST FLAG {@code apply(String)} once: the compiler also emits a synthetic bridge
 * {@code apply(Object)}, which must not be reported a second time. The private {@code stampHeader}
 * is not made async by the class-level annotation.
 */
@Async
public final class ExportService implements Function<String, String> {

    @Override
    public String apply(String document) {
        return stampHeader() + document;
    }

    private String stampHeader() {
        return "exported: ";
    }
}
