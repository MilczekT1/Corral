package io.github.milczekt1.corral.rules.java.nointernalapi;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may depend on anything in {@code sun..} or {@code jdk.internal..}.
 *
 * <p>Stands alone, in no group: wire it with {@code ArchTests.in(NoInternalApiRule.class)}. Reflective
 * access by a string name leaves no bytecode dependency and is not seen.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoInternalApiRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.java.no-internal-api")
            .why("""
                    sun.. and jdk.internal.. are JDK implementation packages with no compatibility \
                    promise. The ones that still compile against a modern release are those the \
                    jdk.unsupported module deliberately exports — sun.misc.Unsafe, sun.misc.Signal, \
                    sun.reflect.ReflectionFactory — and they compile with only a "proprietary API" \
                    warning that @SuppressWarnings cannot silence, so every build prints it and every \
                    reader learns to skip it. That warning is the only notice before the API goes: \
                    Unsafe's memory-access methods are deprecated for removal since JDK 23 (JEP 471) and \
                    warn at runtime by default since JDK 24 (JEP 498), and a later release makes them \
                    throw. A routine JDK upgrade then fails in whichever code path touches them. The \
                    rest of sun.. and jdk.internal.. is not exported at all: javac refuses it under \
                    --release, so it reaches bytecode only through -source/-target plus --add-exports, \
                    and then throws IllegalAccessError on any JVM started without the matching flag.""")
            .howToFix("""
                    Use the supported replacement. Unsafe compare-and-swap and volatile field access \
                    become a VarHandle from MethodHandles.lookup().findVarHandle, or a \
                    java.util.concurrent.atomic type. Unsafe.park and unpark become LockSupport. \
                    Off-heap allocation becomes ByteBuffer.allocateDirect, or \
                    java.lang.foreign.MemorySegment on JDK 22 and later. If a sun.misc.Signal handler \
                    only stops the process cleanly on SIGTERM or SIGINT, Runtime.addShutdownHook \
                    replaces it. When no replacement exists — a custom signal such as SIGHUP, or \
                    ReflectionFactory in a serialization framework — move every such call into one \
                    class in one package, so the next JDK upgrade has a single file to fix, and exclude \
                    that package with a reason saying which API has no replacement.""")
            .howNotToFix("""
                    Do NOT replace the reference with Class.forName("sun.misc.Unsafe"), a MethodHandle \
                    looked up by name, or any other string-named reflective access: the bytecode \
                    dependency disappears and this rule does not catch the reflective form, but the call \
                    fails on exactly the same JDK release, and nothing will warn again. Do NOT wrap the \
                    internal type in a class of your own to hide it: the wrapper depends on it and is \
                    flagged. Do NOT copy the JDK class's source into your own package: it is \
                    GPL-licensed and depends on further internals. Do NOT pin the project to an older \
                    JDK to keep the call working.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("sun..", "jdk.internal..");

    @ArchTest
    public static final ArchRule rule = new NoInternalApiRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
