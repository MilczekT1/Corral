package io.github.milczekt1.corral.rules.java.nolegacysynchronizedcollections;

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
 * declared, never where it is used.
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
                    Vector becomes ArrayList. Stack becomes ArrayDeque used through push/pop/peek — \
                    check the iteration order at every call site, because that is where the silent \
                    behaviour change lives. Hashtable becomes HashMap, or ConcurrentHashMap if the \
                    concurrency was real; check for code relying on the NullPointerException for null \
                    and on iteration order. Where the synchronization was genuinely load-bearing, \
                    ConcurrentHashMap, CopyOnWriteArrayList and ConcurrentLinkedDeque provide it with \
                    iterators that never throw ConcurrentModificationException. java.util.Properties \
                    extends Hashtable but is a distinct class and is not matched. Where an API forces \
                    one of the three types on you — a JNDI InitialContext environment, a Swing \
                    JComboBox, JList or DefaultTableModel — keep that interop in one class and exclude \
                    it with a reason naming the API.""")
            .howNotToFix("""
                    Do NOT wrap the replacement in Collections.synchronizedList or synchronizedMap to \
                    keep the old locking: that reproduces the per-method locking that does not make \
                    compound operations atomic, adds the duty to hold the collection's monitor while \
                    iterating, and this rule does not catch it. Do NOT hide the type behind a subclass \
                    (class UndoStack extends Stack) or a factory method in a helper class: the subclass \
                    declaration and the helper are flagged, but code using them is not, so once those \
                    are frozen every new use through them is invisible. Do NOT declare the field as \
                    List while still calling new Vector<>(): the constructor call is a dependency and \
                    is matched.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .should().dependOnClassesThat().belongToAnyOf(Vector.class, Stack.class, Hashtable.class);

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
