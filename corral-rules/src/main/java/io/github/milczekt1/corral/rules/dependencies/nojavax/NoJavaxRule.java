package io.github.milczekt1.corral.rules.dependencies.nojavax;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may depend on anything in {@code javax..}.
 *
 * <p>Stands alone, in no group: wire it with {@code ArchTests.in(NoJavaxRule.class)}, the same way a
 * group names it. It also flags {@code javax} packages that never moved to {@code jakarta}, JDK APIs
 * such as {@code javax.sql} included.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoJavaxRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.dependencies.no-javax")
            .why("""
                    A codebase that has finished moving to Jakarta EE regresses one import at a time: an \
                    EE 8 snippet or a transitive javax artifact brings back javax.persistence, \
                    javax.validation, javax.servlet or javax.annotation, and a jakarta runtime ignores \
                    them without an error — a javax @PostConstruct is never called, a javax @Entity is \
                    never mapped, a javax @NotNull is never validated. This rule bans the whole javax \
                    namespace rather than a list of migrated packages, so no package can be missed. That \
                    also flags javax packages that never moved: JDK APIs such as javax.sql.DataSource, \
                    javax.crypto, javax.net.ssl and javax.naming, and libraries such as JSR-305's \
                    javax.annotation.Nullable, javax.cache and javax.money. It fits only a codebase that \
                    has decided to use no javax at all, which is why it ships in no group.""")
            .howToFix("""
                    If the type has a jakarta counterpart, switch the import and the dependency to it, \
                    then confirm the annotation still takes effect: a lifecycle callback, mapping or \
                    constraint on the wrong namespace fails without an error. Find what brings the javax \
                    artifact in with mvn dependency:tree. If the type has no jakarta counterpart — a JDK \
                    API or a library that never migrated — this rule does not fit the codebase: stop \
                    wiring it rather than rewriting working code around it.""")
            .howNotToFix("""
                    Do NOT wrap the javax type in a class of your own to hide the import: the wrapper \
                    depends on javax and is flagged. Do NOT reach the type reflectively to dodge the \
                    predicate: that hides the dependency without removing it. Three things this predicate \
                    does NOT catch: a catch of a javax exception that calls nothing on it, because a catch \
                    clause alone records no dependency; javax names in XML or properties files, which are \
                    not bytecode; and source-retention annotations such as javax.annotation.Generated, \
                    which leave nothing in the class file.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .should().dependOnClassesThat().resideInAPackage("javax..");

    @ArchTest
    public static final ArchRule rule = new NoJavaxRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
