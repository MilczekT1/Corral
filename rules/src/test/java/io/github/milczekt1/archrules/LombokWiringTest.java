package io.github.milczekt1.archrules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.archrules.format.AntiFixPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards against Lombok silently generating nothing.
 *
 * <p>Misconfigured annotation processing produces no code and still reports BUILD SUCCESS. With
 * {@code @UtilityClass} the damage is invisible: the hand-written private constructor is gone, so
 * Java supplies an implicit <em>public</em> one and the class becomes instantiable.
 *
 * <p>These assertions anchor on the shape {@code @UtilityClass} produces, which nothing else now
 * produces. Run with {@code clean} — stale classes mask the failure.
 */
class LombokWiringTest {

    private static final List<Class<?>> UTILITY_CLASSES =
            List.of(RuleRegistry.class, FrozenRules.class, AntiFixPolicy.class);

    @Test
    void utilityClassesAreFinal() {
        for (Class<?> type : UTILITY_CLASSES) {
            assertTrue(Modifier.isFinal(type.getModifiers()),
                    type.getSimpleName() + " must be final — @UtilityClass did not run");
        }
    }

    @Test
    void utilityClassesCannotBeInstantiated() {
        for (Class<?> type : UTILITY_CLASSES) {
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            assertEquals(1, constructors.length,
                    type.getSimpleName() + " should declare exactly one constructor");
            assertTrue(Modifier.isPrivate(constructors[0].getModifiers()),
                    type.getSimpleName() + " has a non-private constructor — @UtilityClass did not run,"
                            + " so Java supplied an implicit public one");
        }
    }

    @Test
    void utilityClassMembersRemainStatic() {
        for (Class<?> type : UTILITY_CLASSES) {
            for (var method : type.getDeclaredMethods()) {
                if (method.isSynthetic()) {
                    continue;
                }
                assertTrue(Modifier.isStatic(method.getModifiers()),
                        type.getSimpleName() + "." + method.getName() + " must be static");
            }
        }
    }
}
