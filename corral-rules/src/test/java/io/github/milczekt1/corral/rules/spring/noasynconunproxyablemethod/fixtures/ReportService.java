package io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures;

import org.springframework.scheduling.annotation.Async;

/**
 * MUST FLAG {@code render} only: the class-level annotation makes it async. It never reaches the
 * private {@code formatRows} or {@code trimRows}, nor the static {@code pageSize}, and
 * {@code exportCsv} is overridable.
 */
@Async
public class ReportService {

    private int rows;

    public final void render() {
        rows = formatRows() + trimRows();
    }

    public void exportCsv() {
        rows += pageSize();
    }

    static int pageSize() {
        return 50;
    }

    private int formatRows() {
        return rows + 1;
    }

    private final int trimRows() {
        return rows - 1;
    }
}
