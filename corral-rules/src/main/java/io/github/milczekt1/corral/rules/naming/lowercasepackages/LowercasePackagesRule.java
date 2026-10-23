package io.github.milczekt1.corral.rules.naming.lowercasepackages;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No top-level class, production or test, may reside in a package whose name contains an uppercase
 * letter. The default package passes.
 *
 * <p>Test classes are checked only when the consumer's import includes them:
 * {@code ImportOption.DoNotIncludeTests} drops them.
 *
 * <p>Stands alone, in no group: wire it with {@code ArchTests.in(LowercasePackagesRule.class)}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LowercasePackagesRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.naming.packages-must-be-lowercase")
            .why("""
                    Package segments are directories on disk, and the default filesystems on macOS \
                    (APFS) and Windows (NTFS) ignore case. Once a package name has uppercase letters, \
                    someone can add com/acme/model next to com/acme/Model. That builds on a Linux CI \
                    runner, but on a laptop git shows a change that will not go away, or one directory \
                    absorbs the other. Mixed case also blurs a fully-qualified name: in \
                    com.acme.Model.Order a reader cannot tell where the package ends and the class \
                    begins.""")
            .howToFix("""
                    Rename the package to lowercase: com.acme.orderprocessing, or split the words into \
                    segments, com.acme.order.processing. Underscores and digits are accepted. Rename \
                    through the IDE, so the directory, imports and package-info.java move with the \
                    package line. Then check that git status shows the rename: on macOS and Windows, git \
                    may not record one that only changes case. If it does not, record it in two steps \
                    through a temporary name: git mv <parent>/orderProcessing <parent>/tmp && git mv \
                    <parent>/tmp <parent>/orderprocessing. Top-level classes and an annotated \
                    package-info are reported; nested and anonymous classes move with them. Generated \
                    code is fixed where its package is chosen: an xjc binding file or a jOOQ generator \
                    strategy in your own build is yours to change. A package copied verbatim from a \
                    schema you do not control — a .proto file's java_package, or its package when \
                    java_package is absent, or a <jaxb:package> inside an XSD another team owns — cannot \
                    be renamed here, and every top-level type that schema adds later is a new violation: \
                    a codebase compiling such a schema should not wire this rule, or should list it in \
                    corral-exclusions.txt with that reason.""")
            .howNotToFix("""
                    Do NOT change only the package line and leave the directory in mixed case. Maven \
                    still compiles it, so this rule passes — it reads class files and cannot see \
                    directories — but the collision on disk is unchanged. Do NOT exclude packages from \
                    the @AnalyzeClasses scan: the rule goes quiet, nothing on disk changes, and this rule \
                    cannot tell.""")
            .build();

    static final ArchRule DEFINITION = classes()
            .that().areTopLevelClasses()
            .should(resideInALowercasePackage());

    @ArchTest
    public static final ArchRule rule = new LowercasePackagesRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }

    private static ArchCondition<JavaClass> resideInALowercasePackage() {
        return new ArchCondition<>("reside in a lowercase package") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String packageName = javaClass.getPackageName();
                if (!packageName.equals(packageName.toLowerCase(Locale.ROOT))) {
                    events.add(SimpleConditionEvent.violated(javaClass, "Class <" + javaClass.getFullName()
                            + "> resides in package <" + packageName + "> in "
                            + javaClass.getSourceCodeLocation()));
                }
            }
        };
    }
}
