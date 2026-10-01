package io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures;

/** MUST FLAG: a private method carrying a composed annotation meta-annotated {@code @Async}. */
public class AuditService {

    private int records;

    @BackgroundTask
    private void recordAudit() {
        records++;
    }

    public void audit() {
        recordAudit();
    }
}
