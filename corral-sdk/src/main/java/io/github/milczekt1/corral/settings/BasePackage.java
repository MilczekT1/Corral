package io.github.milczekt1.corral.settings;

import static io.github.milczekt1.corral.settings.CorralSettings.BASE_PACKAGE;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.PackageMatcher;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.function.Supplier;
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
}
