package io.github.milczekt1.corral.rules.spring.noasynconunproxyablemethod.fixtures;

import org.springframework.scheduling.annotation.Async;

/**
 * MUST IGNORE: an async superclass with no methods of its own; its annotation reaches every subclass's
 * instance methods.
 */
@Async
public abstract class AsyncWorkerBase {

    protected int queued;
}
