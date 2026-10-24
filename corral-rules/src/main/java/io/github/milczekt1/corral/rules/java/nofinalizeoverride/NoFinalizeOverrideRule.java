package io.github.milczekt1.corral.rules.java.nofinalizeoverride;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class or interface may declare a no-arg {@code finalize()} method.
 *
 * <p>Production and test classes alike. An empty {@code final} finalizer is flagged too: the body is
 * not read.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoFinalizeOverrideRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.java.no-finalize-override")
            .why("""
                    Object.finalize() is deprecated since Java 9 and deprecated for removal since Java \
                    18 (JEP 421); --finalization=disabled already turns it off, and code relying on it \
                    then stops working with no error. A finalizer is not a destructor: the JVM may exit \
                    without running it, so a finalizer that releases a file handle or native memory is \
                    a leak. When it does run, it runs late, on one JVM-wide Finalizer thread, in no \
                    order. Instances survive an extra GC cycle, and one slow finalizer backs up the \
                    queue: the JVM can die of OutOfMemoryError with a heap full of unreachable \
                    objects. An exception thrown from it is silently discarded and the rest of the \
                    method skipped. It runs with a live this, so it can resurrect the object, and a \
                    subclass finalizer can capture a partially-constructed instance whose constructor \
                    threw.""")
            .howToFix("""
                    Implement AutoCloseable and release the resource in try-with-resources at each call \
                    site; this covers almost every finalizer. If you also need a backstop for callers \
                    who forget, register with java.lang.ref.Cleaner and call Cleanable.clean() from \
                    close(). The cleaning action must not reference the object, so no lambda or inner \
                    class capturing this, or the object never becomes unreachable and the action never \
                    runs: a static nested class holding only the resource is the safe shape. This rule \
                    does not read the cleaning action. On Java 22 and later, scope native memory with \
                    java.lang.foreign.Arena. An empty final finalize() written only to stop a subclass \
                    installing a finalizer is flagged too, because this rule does not read the body: \
                    freeze the ones that exist as debt. A new class needing that protection is made \
                    final, or validates its arguments in a static method whose result is passed to \
                    this(...), so the constructor throws before Object's constructor runs and no \
                    finalizer is registered for the partial instance.""")
            .howNotToFix("""
                    Do NOT delete the finalizer unless callers actually call close(): the leak goes from \
                    sometimes to always, and this rule passes either way. Do NOT wrap the body in \
                    try/catch (Throwable): it may still never run, and this rule still flags it. Do NOT \
                    add @SuppressWarnings("removal") or ("deprecation"), or -Xlint:-removal or \
                    -Xlint:-deprecation: \
                    that hides the compiler warning, and this rule still flags the method. Do NOT \
                    replace it with an empty protected final void finalize() {}: this rule flags that \
                    too. Do NOT rename it to a dispose() nothing calls, or poll a hand-rolled \
                    PhantomReference queue: this rule does not flag either, the first releases nothing \
                    and the second is Cleaner written worse. Not caught: calls to \
                    System.runFinalization() or Runtime.runFinalization().""")
            .build();

    /**
     * Declared methods only, so a class merely inheriting {@code Object.finalize} is never matched. Do
     * not move the parameter check into {@code andShould}: each line would then print the method twice.
     */
    static final ArchRule DEFINITION = noMethods()
            .that().haveRawParameterTypes(new Class<?>[0])
            .should().haveName("finalize");

    @ArchTest
    public static final ArchRule rule = new NoFinalizeOverrideRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
