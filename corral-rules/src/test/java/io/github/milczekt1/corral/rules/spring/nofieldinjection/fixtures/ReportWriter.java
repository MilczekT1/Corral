package io.github.milczekt1.corral.rules.spring.nofieldinjection.fixtures;

import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;

/**
 * MUST FLAG {@code source}, {@code jakarta.annotation.Resource} by name. MUST IGNORE
 * {@code footer}: {@code @Nullable} shares {@code @Resource}'s package, so a predicate widened to
 * the package fails. No stereotype annotation: the wiring is wrong however the bean is registered.
 */
public class ReportWriter {

    @Resource(name = "reportsRepository")
    private OrderRepository source;

    @Nullable
    private String footer;

    public String write() {
        return source.count() + (footer == null ? "" : footer);
    }
}
