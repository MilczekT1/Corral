package io.github.milczekt1.corral.rules.spring.notransactionalonprivatemethod.fixtures;

/** MUST FLAG: a private method carrying a composed annotation meta-annotated {@code @Transactional}. */
public class ReportService {

    private int totals;

    public int totals() {
        loadTotals();
        return totals;
    }

    @ReadOnlyTransaction
    private void loadTotals() {
        totals = 42;
    }
}
