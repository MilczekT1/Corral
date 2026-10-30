package io.github.milczekt1.corral.settings;

import static io.github.milczekt1.corral.settings.CorralSettings.BASE_PACKAGE;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/** Predicates over {@value CorralSettings#BASE_PACKAGE} whose descriptions never contain its value. */
@UtilityClass
public class BasePackage {

    /**
     * Classes whose package matches {@code packagePattern} appended to the configured base package,
     * e.g. {@code "..service.."}. {@code doc} must declare {@value CorralSettings#BASE_PACKAGE}.
     */
    public static DescribedPredicate<JavaClass> resideIn(RuleDoc doc, String packagePattern) {
        return resideIn(doc, packagePattern, CorralSettings::current);
    }

    static DescribedPredicate<JavaClass> resideIn(
            RuleDoc doc, String packagePattern, Supplier<SettingsFile> source) {
        Supplier<String> basePackage = CorralSettings.setting(doc, BASE_PACKAGE, source);
        return DescribedPredicate.describe(
                "reside in the configured base package, matching '" + packagePattern + "'",
                javaClass -> PackageMatcher.of(basePackage.get() + packagePattern)
                        .matches(javaClass.getPackageName()));
    }

    /**
     * {@code rule} unchanged unless {@code doc} declares {@value CorralSettings#BASE_PACKAGE}; otherwise
     * a rule that fails when no imported class resides in the configured base package. Apply inside
     * {@code FreezingArchRule}: a base package that matches nothing passes every rule reading it, and
     * the freeze would record their debt as resolved.
     */
    public static ArchRule requireMatchingClasses(ArchRule rule, RuleDoc doc) {
        return requireMatchingClasses(rule, doc, CorralSettings::current);
    }

    static ArchRule requireMatchingClasses(ArchRule rule, RuleDoc doc, Supplier<SettingsFile> source) {
        return doc.requiredSettings().contains(BASE_PACKAGE) ? new Covered(rule, source) : rule;
    }

    /** A missing value is left to {@code RequiredSettings}, which fails the rule before it gets here. */
    @RequiredArgsConstructor
    private static final class Covered implements ArchRule {

        private final ArchRule delegate;
        private final Supplier<SettingsFile> source;

        @Override
        public void check(JavaClasses classes) {
            throwWhenUnmatched(classes);
            delegate.check(classes);
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            throwWhenUnmatched(classes);
            return delegate.evaluate(classes);
        }

        private void throwWhenUnmatched(JavaClasses classes) {
            Optional<String> basePackage = source.get().value(BASE_PACKAGE);
            if (basePackage.isEmpty()) {
                return;
            }
            PackageMatcher matcher = PackageMatcher.of(basePackage.get() + "..");
            if (classes.stream().noneMatch(javaClass -> matcher.matches(javaClass.getPackageName()))) {
                throw new AssertionError("""
                        %s is '%s', but no imported class resides in it. A base package that matches \
                        nothing passes every rule that reads it and erases their frozen debt. Fix the \
                        value in src/test/resources/%s.\
                        """.formatted(BASE_PACKAGE, basePackage.get(), CorralSettings.FILE));
            }
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public ArchRule as(String newDescription) {
            return new Covered(delegate.as(newDescription), source);
        }

        @Override
        public ArchRule because(String reason) {
            return new Covered(delegate.because(reason), source);
        }

        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return new Covered(delegate.allowEmptyShould(allowEmptyShould), source);
        }
    }
}
