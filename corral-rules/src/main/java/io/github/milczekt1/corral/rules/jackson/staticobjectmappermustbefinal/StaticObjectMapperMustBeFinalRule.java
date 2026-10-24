package io.github.milczekt1.corral.rules.jackson.staticobjectmappermustbefinal;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A static field of a production class whose raw type is assignable to Jackson 2's or Jackson 3's
 * {@code ObjectMapper} must be {@code final}.
 *
 * <p>Matched by type name, so it loads without Jackson. When ArchUnit cannot resolve Jackson's
 * hierarchy — Jackson absent from the classpath the test runs on, or
 * {@code resolveMissingDependenciesFromClassPath=false} — a field typed a Jackson subclass such as
 * {@code JsonMapper}, or a project class extending one, is not checked; a field typed exactly
 * {@code ObjectMapper}, or a project class extending it directly, still is.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StaticObjectMapperMustBeFinalRule implements DocumentedRule {

    static final String JACKSON_2_OBJECT_MAPPER = "com.fasterxml.jackson.databind.ObjectMapper";

    static final String JACKSON_3_OBJECT_MAPPER = "tools.jackson.databind.ObjectMapper";

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.jackson.static-object-mapper-must-be-final")
            .why("""
                    A static mapper is shared by every thread and every test in the JVM. If the field \
                    is not final, any code can swap it: a test that wants pretty output assigns \
                    mapper.copy().enable(INDENT_OUTPUT), or a setter lets one code path change \
                    serialisation for everyone. The swap outlives the test that made it, so a later \
                    test in the same JVM fails in a different file, depending on test order; in \
                    production a request can serialise with one configuration and its retry with \
                    another. On Jackson 2 there is a second defect: a mapper is configured by mutating \
                    it, with no synchronisation. With lazy initialisation — mapper = new \
                    ObjectMapper(); mapper.registerModule(new JavaTimeModule()); — another thread can \
                    see the new reference before the module is registered. From Jackson 2.12 it then \
                    fails with "Java 8 date/time type `java.time.Instant` not supported by default" \
                    (earlier 2.x writes the value in a bean shape instead), from a mapper that looks \
                    fully configured by the time anyone checks. A static final field assigned in its \
                    declaration is published safely by class initialisation. Jackson 3 mappers are \
                    immutable, so only the swap applies there.""")
            .howToFix("""
                    If the mapper comes from a container — a setter or @PostConstruct copying an \
                    injected mapper into the static field — inject it through the constructor into a \
                    private final instance field and pass it to whoever used the static one; do not \
                    replace it with a mapper you build yourself. Otherwise build the mapper once and \
                    assign it once, in the declaration: private static final ObjectMapper MAPPER = \
                    JsonMapper.builder()...build();. On Jackson 2, add modules on the builder \
                    (.addModule(new JavaTimeModule())), not on the mapper afterwards. On Jackson 2.9 or \
                    earlier, which has no JsonMapper.builder(), have the declaration call a private \
                    static method that builds and configures it: private static final ObjectMapper \
                    MAPPER = createMapper();. On Jackson 3, java.time support is built in. A static \
                    final initialiser is already lazy and thread-safe and needs no lock. In a Kotlin \
                    companion object, declare it val, not var or lateinit var. If a call site needs \
                    different settings, derive them instead of reassigning: mapper.writer().with(...) \
                    and mapper.readerFor(Type.class) return immutable, thread-safe views that can be \
                    their own static final fields. A configuration holder that rebuilds its mapper \
                    when configuration is reloaded is a legitimate exception: freeze it as debt. Only \
                    static fields are checked; instance fields, including injected ones, are not, and \
                    neither are test classes — by output location or a declared test method, never by \
                    name. A shared test-support module's main sources and Gradle testFixtures or \
                    integrationTest output are production output, so a test base class there holding \
                    a static mapper is flagged unless it declares a test method itself.""")
            .howNotToFix("""
                    Do NOT replace a mapper the container injected with a static final one you build \
                    yourself: this rule passes, but the new mapper has none of the modules and features \
                    the container configured, so dates, unknown properties and property naming can \
                    serialise differently with no error. Do NOT make the field volatile or add a \
                    synchronized lazy initialiser: the field stays non-final, so this rule still fails, \
                    and the mapper can still be swapped. Do NOT make the field final and keep \
                    configuring the mapper afterwards (MAPPER.registerModule(...) in an init() method): \
                    this rule does not catch that, and on Jackson 2 it is the same race and the same \
                    shared change. Do NOT hold the mapper in a static final \
                    AtomicReference<ObjectMapper> or a Supplier: this rule does not catch that, and the \
                    mapper can still be swapped. Do NOT retype the field to Object or to a supertype \
                    the mapper implements — ObjectCodec, TreeCodec, Versioned — or move the mapper \
                    behind a facade held in a mutable static field: this rule does not catch that \
                    either, and nothing about the mapper's lifecycle changed. Do NOT build a new \
                    mapper at each call site: this rule does not catch it, and it rebuilds an \
                    expensive object on every call.""")
            .build();

    static final ArchRule DEFINITION = fields()
            .that().areDeclaredInClassesThat(TestScope.PRODUCTION_CLASSES)
            .and().areStatic()
            .and().haveRawType(assignableTo(JACKSON_2_OBJECT_MAPPER).or(assignableTo(JACKSON_3_OBJECT_MAPPER)))
            .should().beFinal();

    @ArchTest
    public static final ArchRule rule = new StaticObjectMapperMustBeFinalRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
