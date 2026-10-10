package io.github.milczekt1.corral.rules.testing.nolegacymockbeanandspybean;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No test class may depend on Spring Boot's {@code org.springframework.boot.test.mock.mockito}
 * package, which Boot 3.4.0 deprecated for removal and Boot 4.0 removed.
 *
 * <p>Inspects <em>test</em> classes, so consumers must not set
 * {@code ImportOption.DoNotIncludeTests} — it would pass vacuously.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoLegacyMockBeanAndSpyBeanRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.test.no-legacy-mockbean-and-spybean")
            .why("""
                    Every public type in org.springframework.boot.test.mock.mockito — @MockBean, \
                    @SpyBean, their @MockBeans and @SpyBeans containers, MockReset — is deprecated for \
                    removal since Spring Boot 3.4.0, and the package does not exist in Spring Boot 4.0. \
                    A test using it compiles on 3.4 and 3.5 behind a warning nobody reads, then stops \
                    compiling on the Boot 4 upgrade together with every other such test, in one diff. \
                    Each new use written where the replacement already exists is migration work \
                    created for nothing.""")
            .howToFix("""
                    Replace @MockBean with \
                    org.springframework.test.context.bean.override.mockito.MockitoBean and @SpyBean \
                    with MockitoSpyBean (Spring Framework 6.2, so Spring Boot 3.4 or later). The \
                    attributes do not map one to one. value is now the bean NAME, a String, not a \
                    type: @MockBean(Foo.class) becomes @MockitoBean(types = Foo.class), and classes \
                    also becomes types. answer becomes answers. MockReset moved to \
                    org.springframework.test.context.bean.override.mockito. @SpyBean's \
                    proxyTargetAware has no counterpart. @MockitoSpyBean wraps a bean that already \
                    exists in the context and does not create one. Bean-override fields are not \
                    supported on @Configuration or @TestConfiguration classes: move them onto the test \
                    class or a superclass of it. While you are there, ask whether the test needs a \
                    Spring context at all — a mock passed into a constructor needs no container.""")
            .howNotToFix("""
                    Do NOT switch to a plain @Mock field pushed into the context with \
                    ReflectionTestUtils: that bypasses the container's override mechanism and leaves \
                    the real bean wired everywhere else — this rule does NOT catch it. Do NOT pin \
                    spring-boot-test to an older version to keep the annotations alive — this rule \
                    does NOT catch that either, and it blocks the Boot upgrade. Do NOT delete the test \
                    because the migration looks fiddly — not caught. Suppressing the deprecation \
                    warning or adding @SuppressWarnings does not silence this rule. Do NOT hide \
                    @MockBean inside a project's own composed annotation: dependOnClassesThat is not \
                    transitive, so the composed annotation's declaration is flagged and the classes \
                    using it are not — and Boot 4 still fails to compile it. Do NOT hand-edit the \
                    freeze store to admit a new entry.""")
            .build();

    static final String LEGACY_PACKAGE = "org.springframework.boot.test.mock.mockito..";

    /** Package-matched, so the rule loads on Boot 4, where none of these types exist. */
    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.TEST_CLASSES)
            .should().dependOnClassesThat().resideInAPackage(LEGACY_PACKAGE);

    @ArchTest
    public static final ArchRule rule = new NoLegacyMockBeanAndSpyBeanRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
