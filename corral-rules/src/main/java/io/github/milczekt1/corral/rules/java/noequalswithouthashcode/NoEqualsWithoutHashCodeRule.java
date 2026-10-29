package io.github.milczekt1.corral.rules.java.noequalswithouthashcode;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A class that declares a concrete {@code equals(Object)} must have a no-arg {@code hashCode()}
 * declared on itself or on a superclass other than {@code Object}.
 *
 * <p>Production and test classes alike. A superclass outside the imported classes is read only while
 * {@code resolveMissingDependenciesFromClassPath} is left {@code true}; with it off, a class inheriting
 * {@code hashCode()} from one is flagged.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoEqualsWithoutHashCodeRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.java.no-equals-without-hashcode")
            .why("""
                    Object's contract requires equal objects to have equal hash codes. Override \
                    equals(Object) alone and hashCode() stays identity-based, so two equal instances land \
                    in different buckets: HashSet.contains returns false for an element that is in the \
                    set and the set holds duplicates, and HashMap.get returns null for a key that is \
                    present, so caches miss and lookups fail. Nothing throws; the bug surfaces as a \
                    missed cache hit or a duplicated row, far from the class that caused it. javac warns \
                    only with -Xlint:overrides or -Xlint:all, which are off by default.""")
            .howToFix("""
                    Declare public int hashCode() over exactly the fields equals compares, for example \
                    return Objects.hash(a, b, c). If the class is a plain value carrier, make it a record \
                    and delete the hand-written equals; the compiler then generates both over every \
                    component. With Lombok, delete the hand-written equals and use @EqualsAndHashCode, \
                    @Value or @Data: Lombok generates neither method while one of them is hand-written. \
                    Only if equals is identity equality (this == o), which keeps the contract but is \
                    still flagged, declare hashCode() returning super.hashCode(). In an abstract class that leaves hashing to subclasses, declare \
                    public abstract int hashCode(), so every concrete subclass must implement it. A \
                    hashCode() inherited from a superclass other than Object satisfies this rule, which \
                    is correct when the subclass's equals compares at least the fields the inherited \
                    hashCode reads. Freeze \
                    existing violations as debt rather than excluding the rule, even for a class used \
                    only in sorted or identity-hashed collections today.""")
            .howNotToFix("""
                    Do NOT return a constant from hashCode(): it keeps the contract but puts every \
                    instance in one bucket, and this rule does not catch it, because it checks that a \
                    hashCode() exists, never what it computes. Do NOT hash fields that equals ignores: \
                    equal objects then hash differently, and this rule does not catch that either. Do NOT \
                    return super.hashCode() or System.identityHashCode(this) when equals compares \
                    fields: that is identity hashing again, and this rule does not catch it. Do NOT add \
                    @SuppressWarnings("overrides") or -Xlint:-overrides: that hides the javac warning, \
                    and this rule still flags the class. Do NOT add an overload such as hashCode(int): \
                    hash collections never call it, and this rule still flags the class. Do NOT make it \
                    a record and keep a hand-written equals that compares fewer components, or compares \
                    them differently: the generated hashCode() reads every component, and this rule does \
                    not catch it. Do NOT delete equals, or narrow it to an overload such as \
                    equals(Money), to silence the rule without checking the callers that rely on value \
                    equality: hash collections and Objects.equals call equals(Object), so they switch to \
                    identity equality with no error, and this rule catches neither.""")
            .build();

    static final ArchRule DEFINITION = classes().should(haveHashCodeWhenTheyOverrideEquals());

    @ArchTest
    public static final ArchRule rule = new NoEqualsWithoutHashCodeRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }

    private static ArchCondition<JavaClass> haveHashCodeWhenTheyOverrideEquals() {
        return new ArchCondition<>(
                "override hashCode() in the class or a superclass when they override equals(Object)") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                javaClass.tryGetMethod("equals", Object.class)
                        .filter(equals -> !equals.getModifiers().contains(JavaModifier.ABSTRACT))
                        .ifPresent(equals -> {
                            if (!hasHashCodeBelowObject(javaClass)) {
                                events.add(SimpleConditionEvent.violated(javaClass, "Class <" + javaClass.getName()
                                        + "> overrides equals(Object) without hashCode() in "
                                        + equals.getSourceCodeLocation()));
                            }
                        });
            }
        };
    }

    /**
     * The class and its superclasses, never interfaces: {@code List} and {@code Set} re-declare an
     * abstract {@code hashCode()}, which implements nothing. Do not drop the {@code Object} filter:
     * {@code Object} declares {@code hashCode()}, so every class would pass.
     */
    private static boolean hasHashCodeBelowObject(JavaClass javaClass) {
        return javaClass.tryGetMethod("hashCode").isPresent()
                || javaClass.getAllRawSuperclasses().stream()
                        .filter(type -> !type.isEquivalentTo(Object.class))
                        .anyMatch(type -> type.tryGetMethod("hashCode").isPresent());
    }
}
