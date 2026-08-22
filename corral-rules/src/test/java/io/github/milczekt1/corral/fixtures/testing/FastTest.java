package io.github.milczekt1.corral.fixtures.testing;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;

/**
 * A project's own composed test annotation — the shape JUnit 5 documents and encourages.
 *
 * <p>JUnit runs methods annotated with this, because it resolves {@code @Test} through the
 * meta-annotation. A rule matching direct annotations only would not.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.METHOD)
@Test
public @interface FastTest {
}
