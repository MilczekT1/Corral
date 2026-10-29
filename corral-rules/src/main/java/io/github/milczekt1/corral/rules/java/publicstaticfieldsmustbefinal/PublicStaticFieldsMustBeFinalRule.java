package io.github.milczekt1.corral.rules.java.publicstaticfieldsmustbefinal;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A {@code public static} field of a production class must be {@code final}, unless the declaring
 * class is a JPA static metamodel ({@code @StaticMetamodel}, jakarta or javax), whose attributes the
 * specification requires to be {@code public static volatile}.
 *
 * <p>Reads the field's modifiers only: the declaring class's own visibility is not read, and a
 * {@code final} field holding a mutable object is not flagged. Compiler-generated fields are not
 * exempt: every Groovy class, enum, closure and trait helper — not interfaces, traits or a Groovy 4
 * {@code @POJO} class — is flagged on the {@code public static transient boolean __$stMC} Groovy
 * adds to it.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PublicStaticFieldsMustBeFinalRule implements DocumentedRule {

    static final String JAKARTA_STATIC_METAMODEL = "jakarta.persistence.metamodel.StaticMetamodel";

    static final String JAVAX_STATIC_METAMODEL = "javax.persistence.metamodel.StaticMetamodel";

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.java.public-static-fields-must-be-final")
            .why("""
                    A public static field that is not final is global state with no owner. Every class \
                    that can see it can reassign it, so its value depends on class-initialisation order \
                    and on code you have never read. A write with no synchronisation gives other \
                    threads no happens-before edge, so a reader can see a stale value or a partly \
                    constructed object. Finding every write means searching the whole classpath, and \
                    reflection and your artifact's consumers are not in that search.""")
            .howToFix("""
                    If the field is never reassigned, add final, and if it holds a collection initialise \
                    it with List.copyOf(...), Set.of(...) or Map.of(...), so it is immutable as well as \
                    final. A field that is reassigned at runtime — a flag, a clock, a configured \
                    endpoint — is not a constant: make it an instance field of a collaborator and inject \
                    that collaborator, so a test can set it without leaking into other tests. Generated \
                    code you cannot edit, such as an ANTLR lexer's modeNames and channelNames or \
                    Groovy's __$stMC, is not fixed by hand: freeze what exists as debt, since its lines \
                    name the field, not a line number, and survive regeneration. A codebase that \
                    compiles Groovy, or an Android module with generated R classes, adds a violation \
                    with every new class: it excludes this rule with a corral.exclude.<rule-id> entry \
                    in corral.properties, which switches it off for the whole build.""")
            .howNotToFix("""
                    Do NOT make the field private, protected or package-private and leave it mutable, \
                    whether behind a public static setter or with no accessor at all: this rule no \
                    longer sees it, and the shared mutable state is still there. Do NOT make the field \
                    final and wrap the value in an AtomicReference or another mutable holder: this rule \
                    does not flag it, and it is the same global state. Do NOT annotate a class that is \
                    not a JPA metamodel with @StaticMetamodel: this rule exempts every class carrying \
                    it, so that dodge is not caught, and the field is the same global state.""")
            .build();

    static final ArchRule DEFINITION = fields()
            .that().areDeclaredInClassesThat(TestScope.PRODUCTION_CLASSES)
            .and().arePublic()
            .and().areStatic()
            .and().areDeclaredInClassesThat().areNotAnnotatedWith(JAKARTA_STATIC_METAMODEL)
            .and().areDeclaredInClassesThat().areNotAnnotatedWith(JAVAX_STATIC_METAMODEL)
            .should().beFinal();

    @ArchTest
    public static final ArchRule rule = new PublicStaticFieldsMustBeFinalRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
