package io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures;

import org.springframework.scheduling.annotation.Async;

/** An async superclass: its annotation reaches every subclass's instance methods. */
@Async
public abstract class AsyncWorkerBase {

    protected int queued;
}
