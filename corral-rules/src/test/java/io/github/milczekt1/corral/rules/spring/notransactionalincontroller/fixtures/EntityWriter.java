package io.github.milczekt1.corral.rules.spring.notransactionalincontroller.fixtures;

/** Not an example: a generic interface, so an override in a controller gets a bridge method. */
public interface EntityWriter<T> {

    T save(T entity);
}
