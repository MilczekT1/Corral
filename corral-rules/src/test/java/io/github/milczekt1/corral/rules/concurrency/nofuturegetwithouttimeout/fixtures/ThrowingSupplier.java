package io.github.milczekt1.corral.rules.concurrency.nofuturegetwithouttimeout.fixtures;

/**
 * MUST IGNORE: a non-{@code Future} type with a no-argument {@code get()}, and the functional type a
 * {@code future::get} reference is handed to.
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {

    T get() throws Exception;
}
