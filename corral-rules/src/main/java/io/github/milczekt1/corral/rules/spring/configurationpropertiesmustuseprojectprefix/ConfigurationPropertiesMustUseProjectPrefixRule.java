package io.github.milczekt1.corral.rules.spring.configurationpropertiesmustuseprojectprefix;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import io.github.milczekt1.corral.settings.CorralSettings;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Every {@code @ConfigurationProperties} prefix a production class declares — on the class itself, or
 * on a method whose raw return type is an imported production class — must equal
 * {@value CorralSettings#PROPERTY_PREFIX} or start with it followed by a dot. One violation per
 * declaration site.
 *
 * <p>Direct annotations only: a subclass is never reported for its parent's prefix, and a custom
 * annotation carrying {@code @ConfigurationProperties} is reported at its own declaration, never at its
 * uses, so a prefix it forwards through {@code @AliasFor} is not seen. A method returning a type outside
 * the imported classes is skipped, so a library type bound to its own namespace passes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConfigurationPropertiesMustUseProjectPrefixRule implements DocumentedRule {

    private static final String CONFIGURATION_PROPERTIES =
            "org.springframework.boot.context.properties.ConfigurationProperties";

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.configuration-properties-must-use-project-prefix")
            .requiredSetting(CorralSettings.PROPERTY_PREFIX)
            .why("""
                    Without one fixed prefix each properties class picks its own (app, client, kafka), \
                    and nobody can tell which keys in application.yml, a ConfigMap or a config server \
                    belong to this project and which to Spring Boot or a library. An unowned prefix also \
                    shares its keys with everything else that writes configuration. Spring Boot binds \
                    environment variables by relaxed binding, and Kubernetes by default injects \
                    <SERVICE>_PORT=tcp://<ip>:<port> for every Service in the namespace: a \
                    @ConfigurationProperties("kafka") class with an int port then fails to start in the \
                    one cluster that has a kafka Service, and with a String field binds the wrong value \
                    silently. A prefix under server, management or spring can change meaning on a Boot \
                    upgrade. Spring Boot's own advice is to prefix every key with a namespace you own; \
                    corral.property-prefix in corral.properties names this project's.""")
            .howToFix("""
                    Change the prefix so it equals corral.property-prefix from corral.properties, or \
                    starts with it followed by a dot. Rename the keys in every source in the same \
                    change: application.yml and every profile, environment variables, config server, \
                    Helm values, CI variables. Boot ignores unknown keys by default, so a key left under \
                    the old prefix is dropped silently and the field keeps its default. If the keys are \
                    written by someone else, such as a platform namespace every service reads or a \
                    library type's own namespace, do not rename them: their owner keeps writing the old \
                    keys. Leave the violation and report it; this rule has no per-site exception, and \
                    whether it fits this class is a human's call. Only prefixes declared with \
                    @ConfigurationProperties directly on a production class, or on a method whose \
                    declared return type is one of the imported production classes, are checked. A \
                    custom annotation carrying @ConfigurationProperties is checked like any class, at \
                    its own declaration; one that forwards the prefix through @AliasFor is reported \
                    with its own prefix, empty by default, and the prefix each use passes is not seen. \
                    Property files, @Value, @ConditionalOnProperty, Environment and Binder are not \
                    read, and keys you only read from Spring Boot or a library are not in scope.""")
            .howNotToFix("""
                    Do NOT change corral.property-prefix to cover the flagged prefix: it names the one \
                    namespace the whole project uses, and widening it for one class defeats the rule \
                    for every class. Do NOT rename the prefix in code without migrating every \
                    configuration source: the application starts with defaults instead of the \
                    configured values, and nothing reports it. Do NOT replace @ConfigurationProperties \
                    with @Value, Environment or Binder.bind(...): the key is just as unowned, and this \
                    rule does not read any of them. Do NOT move the annotation to a @Bean method \
                    declared to return a library type or a supertype from outside the project: this \
                    rule skips those methods, so it is not caught, and the keys are still unowned. Do \
                    NOT pass the prefix through a custom annotation whose attribute is an @AliasFor of \
                    @ConfigurationProperties' prefix: this rule checks that annotation's own \
                    declaration, not the prefix each use passes, so it is not caught. Do NOT exclude this \
                    rule because existing classes violate it: they are already in the freeze store, and \
                    the rule is there for the next prefix.""")
            .build();

    private static final Supplier<String> PROJECT_PREFIX = CorralSettings.setting(DOC, CorralSettings.PROPERTY_PREFIX);

    static final ArchRule DEFINITION = classes()
            .that(TestScope.PRODUCTION_CLASSES)
            .should(declareConfigurationPropertiesPrefixesUnderTheProjectPrefix());

    @ArchTest
    public static final ArchRule rule = new ConfigurationPropertiesMustUseProjectPrefixRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }

    private static ArchCondition<JavaClass> declareConfigurationPropertiesPrefixesUnderTheProjectPrefix() {
        return new ArchCondition<>("declare @ConfigurationProperties prefixes under the configured property prefix") {

            private Set<String> classesUnderTest = Set.of();

            @Override
            public void init(Collection<JavaClass> allObjectsToTest) {
                classesUnderTest = allObjectsToTest.stream().map(JavaClass::getName).collect(Collectors.toSet());
            }

            @Override
            public void check(JavaClass type, ConditionEvents events) {
                String projectPrefix = PROJECT_PREFIX.get();
                type.tryGetAnnotationOfType(CONFIGURATION_PROPERTIES)
                        .map(ConfigurationPropertiesMustUseProjectPrefixRule::prefixOf)
                        .filter(prefix -> !isUnder(prefix, projectPrefix))
                        .ifPresent(prefix -> events.add(SimpleConditionEvent.violated(type, "Class <"
                                + type.getName() + "> declares @ConfigurationProperties prefix \"" + prefix
                                + "\" outside the configured property prefix in " + type.getSourceCodeLocation())));
                // javac copies a method's annotations onto its bridge methods.
                type.getMethods().stream()
                        .filter(method -> !method.getModifiers().contains(JavaModifier.SYNTHETIC))
                        .filter(method -> classesUnderTest.contains(method.getRawReturnType().getName()))
                        .forEach(method -> method.tryGetAnnotationOfType(CONFIGURATION_PROPERTIES)
                                .map(ConfigurationPropertiesMustUseProjectPrefixRule::prefixOf)
                                .filter(prefix -> !isUnder(prefix, projectPrefix))
                                .ifPresent(prefix -> events.add(SimpleConditionEvent.violated(method, "Method <"
                                        + method.getFullName() + "> declares @ConfigurationProperties prefix \""
                                        + prefix + "\" outside the configured property prefix in "
                                        + method.getSourceCodeLocation()))));
            }
        };
    }

    /** {@code prefix} is an alias of {@code value}; both default to the empty prefix. */
    private static String prefixOf(JavaAnnotation<?> annotation) {
        String value = attribute(annotation, "value");
        return value.isEmpty() ? attribute(annotation, "prefix") : value;
    }

    /** Absent rather than {@code ""} when ArchUnit cannot resolve the annotation type to read its defaults. */
    private static String attribute(JavaAnnotation<?> annotation, String name) {
        return annotation.get(name).map(String.class::cast).orElse("");
    }

    private static boolean isUnder(String prefix, String projectPrefix) {
        return prefix.equals(projectPrefix) || prefix.startsWith(projectPrefix + ".");
    }
}
