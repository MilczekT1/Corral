package io.github.milczekt1.corral.fixtures.testing;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;

/**
 * A project's own composed test annotation — the shape JUnit 5 documents and encourages.
 *
 * <p>JUnit resolves {@code @Test} through the meta-annotation and runs methods annotated with it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.METHOD)
@Test
public @interface FastTest {
}
