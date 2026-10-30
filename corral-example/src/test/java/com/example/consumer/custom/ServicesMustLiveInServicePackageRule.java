package com.example.consumer.custom;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.settings.BasePackage;
import io.github.milczekt1.corral.settings.CorralSettings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A rule this project owns that reads {@code corral.base-package} from {@code corral.properties}
 * rather than hard-coding {@code com.example.consumer}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServicesMustLiveInServicePackageRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("acme.services-must-live-in-service-package")
            .requiredSetting(CorralSettings.BASE_PACKAGE)
            .why("""
                    Services are found by package, not by name: wiring, transaction boundaries and the \
                    stdout rule all assume a service lives under the project's service package.""")
            .howToFix("""
                    Move the class into the service package under the project's base package, or rename \
                    it if it is not a service.""")
            .howNotToFix("""
                    Do NOT change corral.base-package to cover the misplaced class — the setting names \
                    the project's root for every rule that reads it.""")
            .build();

    static final ArchRule DEFINITION = classes()
            .that().haveSimpleNameEndingWith("Service")
            .should(liveInTheServicePackage());

    @ArchTest
    public static final ArchRule rule = new ServicesMustLiveInServicePackageRule().guard();

    /** The violation text names the pattern only: it is a freeze-store key, and the value can change. */
    private static ArchCondition<JavaClass> liveInTheServicePackage() {
        DescribedPredicate<JavaClass> inServicePackage = BasePackage.resideIn(DOC, "..service..");
        return new ArchCondition<>("reside in the service package under the configured base package") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (!inServicePackage.test(javaClass)) {
                    events.add(SimpleConditionEvent.violated(javaClass, javaClass.getDescription()
                            + " does not reside in the service package under the configured base package"));
                }
            }
        };
    }

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
