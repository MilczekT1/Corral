package io.github.milczekt1.corral.rules.java.noclassnewinstance;

import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may call or reference {@code Class.newInstance()}.
 *
 * <p>Production and test classes alike.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoClassNewInstanceRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.java.no-class-new-instance")
            .why("""
                    Class.newInstance() rethrows whatever the constructor throws, checked exceptions \
                    included, without declaring them. Its throws clause names only \
                    InstantiationException and IllegalAccessException, so a constructor's IOException \
                    escapes a caller the compiler believes cannot see one: no catch was written for it, \
                    and javac rejects catch (IOException e) around the call, because nothing in the \
                    try block declares one; only a catch of Exception or Throwable sees it. That is why it was deprecated in Java 9. \
                    Constructor.newInstance() wraps every constructor exception in the declared, \
                    checked InvocationTargetException instead.""")
            .howToFix("""
                    Replace clazz.newInstance() with clazz.getDeclaredConstructor().newInstance(), and \
                    Class::newInstance with a lambda making that call. The replacement finds the same \
                    no-arg constructor and applies the same access check. The replacement wraps every \
                    exception the constructor throws in InvocationTargetException, unchecked ones \
                    included, where the old call threw them as they were. Unwrap it, so callers \
                    catching a specific runtime exception still see it: rethrow getCause() when it is a \
                    RuntimeException or an Error, and wrap anything else in an exception that names the \
                    type being created. A missing no-arg constructor now throws \
                    NoSuchMethodException, not InstantiationException: update any catch that named the \
                    old one.""")
            .howNotToFix("""
                    Do NOT catch ReflectiveOperationException or Exception around the replacement and \
                    wrap it whole: the real cause ends up two levels deep, and runtime exceptions \
                    callers used to catch no longer reach them. This rule cannot see catch blocks, so it \
                    does not flag that. Do NOT add setAccessible(true) to the replacement: the old call \
                    never bypassed access, and this rule does not flag that either. Do NOT move the call \
                    into a helper such as ReflectionUtils.instantiate(Class): the helper is flagged \
                    instead. Do NOT switch to a method reference, Class::newInstance: it is flagged the \
                    same as a call. Not caught: Class.newInstance invoked by reflection, or inside a \
                    library.""")
            .build();

    /**
     * Do not switch to {@code callMethod}: it misses a {@code Class::newInstance} reference. Do not drop
     * the owner: {@code Constructor.newInstance} is the replacement.
     */
    static final ArchRule DEFINITION = noClasses().should().accessTargetWhere(
            target(owner(equivalentTo(Class.class))).and(target(name("newInstance"))));

    @ArchTest
    public static final ArchRule rule = new NoClassNewInstanceRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
