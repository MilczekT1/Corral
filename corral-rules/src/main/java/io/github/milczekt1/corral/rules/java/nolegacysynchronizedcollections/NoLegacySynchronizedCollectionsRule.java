package io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may depend on {@code java.util.Vector}, {@code java.util.Stack} or
 * {@code java.util.Hashtable}.
 *
 * <p>Stands alone, in no group: wire it with
 * {@code ArchTests.in(NoLegacySynchronizedCollectionsRule.class)}. Matched by exact type, so
 * {@code Properties} is not flagged, and a subclass of one of the three is flagged where it is
 * declared, never where it is used. A local variable is not a bytecode dependency, so a
 * {@code Hashtable} read into one and never called, returned or stored in a field is not seen.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoLegacySynchronizedCollectionsRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.java.no-legacy-synchronized-collections")
            .why("""
                    Vector, Stack and Hashtable synchronize every method. That costs a lock on every \
                    get and buys less than it appears to: locking each operation does not make a \
                    sequence of them atomic, so if (!v.contains(x)) v.add(x) is still a race, and \
                    iterating a Vector while another thread adds to it still throws \
                    ConcurrentModificationException. The synchronization reads as a thread-safety \
                    guarantee it does not give, so these types turn up exactly where someone believed \
                    a concurrency problem was solved. Stack extends Vector, so it also exposes add, \
                    remove(int) and get(int), and it iterates bottom-up — the opposite order from \
                    popping. Hashtable throws NullPointerException on a null key or value where \
                    HashMap accepts it, and iterates in a different order.""")
            .howToFix("""
                    Vector becomes ArrayList. Stack becomes ArrayDeque used through push/pop/peek, and \
                    four behaviours change silently at every call site: it iterates top-down where \
                    Stack iterated bottom-up; push(null) throws NullPointerException; peek() on an \
                    empty deque returns null where Stack threw EmptyStackException; and pop() on an \
                    empty deque throws NoSuchElementException, so a catch (EmptyStackException e) \
                    stops catching. Hashtable becomes HashMap; check for code relying on the \
                    NullPointerException for null and on iteration order. Where the concurrency was \
                    real, ConcurrentHashMap, CopyOnWriteArrayList and ConcurrentLinkedDeque iterate \
                    without ConcurrentModificationException, but a check-then-act sequence on them is \
                    still a race: replace containsKey-then-put with putIfAbsent or computeIfAbsent, \
                    and contains-then-add with CopyOnWriteArrayList.addIfAbsent. Most APIs that seem \
                    to demand one of the three do not: a JNDI InitialContext takes its environment as \
                    a Properties, and JComboBox, JList and DefaultTableModel have array constructors. \
                    Exclude a class, with a reason naming the API, only where a signature it must \
                    implement names the type itself.""")
            .howNotToFix("""
                    Do NOT wrap the replacement in Collections.synchronizedList or synchronizedMap to \
                    keep the old locking: that reproduces the per-method locking that does not make \
                    compound operations atomic, adds the duty to hold the collection's monitor while \
                    iterating, and this rule does not catch it. Do NOT replace a Hashtable with \
                    java.util.Properties: it extends Hashtable and keeps its per-method locking, and \
                    this rule does not catch it. Do NOT hide the type behind a subclass \
                    (class UndoStack extends Stack) or a factory method in a helper class: the subclass \
                    declaration and the helper are flagged, but code using them is not, so once those \
                    are frozen every new use through them is invisible. Do NOT declare the field as \
                    List while still calling new Vector<>(): the constructor call is a dependency and \
                    is matched.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .should().dependOnClassesThat(equivalentTo(Vector.class)
                    .or(equivalentTo(Stack.class))
                    .or(equivalentTo(Hashtable.class))
                    .as("are java.util.Vector, java.util.Stack or java.util.Hashtable"));

    @ArchTest
    public static final ArchRule rule = new NoLegacySynchronizedCollectionsRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
