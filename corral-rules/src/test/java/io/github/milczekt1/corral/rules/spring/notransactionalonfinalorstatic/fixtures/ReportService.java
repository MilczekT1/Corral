package io.github.milczekt1.corral.rules.spring.notransactionalonfinalorstatic.fixtures;

/** MUST FLAG: a final method carrying a composed annotation meta-annotated {@code @Transactional}. */
public class ReportService {

    private int totals;

    @ReadOnlyTransaction
    public final int loadTotals() {
        totals = 42;
        return totals;
    }
}
