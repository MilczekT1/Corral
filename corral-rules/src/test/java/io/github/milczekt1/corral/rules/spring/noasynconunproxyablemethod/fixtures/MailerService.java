package io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures;

import org.springframework.scheduling.annotation.Async;

/**
 * MUST FLAG {@code sendDigest} because the class is final, {@code compressDigest} as private and
 * {@code archiveDigest} as final, neither as final-class. {@code preview} is not async.
 */
public final class MailerService {

    private int sent;

    @Async
    public void sendDigest() {
        sent += compressDigest();
    }

    public String preview() {
        return "digests: " + sent;
    }

    @Async
    private int compressDigest() {
        return 1;
    }

    @Async
    public final void archiveDigest() {
        sent = 0;
    }
}
