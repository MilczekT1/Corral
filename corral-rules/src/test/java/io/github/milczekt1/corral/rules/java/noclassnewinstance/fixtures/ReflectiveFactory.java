package io.github.milczekt1.corral.rules.java.noclassnewinstance.fixtures;

import java.util.concurrent.Callable;

/**
 * MUST FLAG {@code fromType}, {@code fromName}, {@code callableOf} and {@code lambdaOf}: each calls or
 * references {@code Class.newInstance}, {@code lambdaOf} from inside a lambda. MUST IGNORE
 * {@code viaConstructor}: {@code Constructor.newInstance} and {@code getDeclaredConstructor}, so a
 * predicate dropping the owner or the name check fails here.
 */
@SuppressWarnings("deprecation")
public class ReflectiveFactory {

    public <T> T fromType(Class<T> type) throws ReflectiveOperationException {
        return type.newInstance();
    }

    public Object fromName(String className) throws ReflectiveOperationException {
        return Class.forName(className).newInstance();
    }

    public <T> Callable<T> callableOf(Class<T> type) {
        return type::newInstance;
    }

    public <T> Callable<T> lambdaOf(Class<T> type) {
        return () -> type.newInstance();
    }

    public <T> T viaConstructor(Class<T> type) throws ReflectiveOperationException {
        return type.getDeclaredConstructor().newInstance();
    }
}
