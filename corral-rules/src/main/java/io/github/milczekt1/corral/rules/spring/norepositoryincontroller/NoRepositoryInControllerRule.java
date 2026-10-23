package io.github.milczekt1.corral.rules.spring.norepositoryincontroller;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import io.github.milczekt1.corral.scope.TestScope;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No production class meta-annotated {@code @Controller} — directly, or through {@code @RestController}
 * or a project's own composed annotation — may depend on a class meta-annotated {@code @Repository} or
 * Spring Data's {@code @RepositoryDefinition}, or on a type assignable to Spring Data's
 * {@code org.springframework.data.repository.Repository}.
 *
 * <p>Stands alone, in no group: wire it with {@code ArchTests.in(NoRepositoryInControllerRule.class)}.
 * Spring's types are named as strings, so the class loads without Spring on the classpath. It needs
 * ArchUnit's default {@code resolveMissingDependenciesFromClassPath=true}: with it off,
 * {@code @RestController} classes and interfaces extending {@code CrudRepository} are not recognised,
 * and the rule reports almost nothing. Only the controller's own code is checked, not a superclass or
 * a nested class. One violation per dependency.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoRepositoryInControllerRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.spring.no-repository-in-controller")
            .why("""
                    A controller that calls repositories has no transaction around its work. Each \
                    Spring Data call runs in its own transaction, or none, so a handler that does two \
                    writes commits the first even when the second fails. The query also skips whatever \
                    authorisation, validation and mapping the service layer applies, so the same data is \
                    reachable by two paths with two sets of rules. And the entity tends to go straight \
                    into the response body, so renaming an entity field changes the HTTP contract.""")
            .howToFix("""
                    Put the call behind a service. Give the service a method named for what the caller \
                    wants, let it own the transaction and the mapping from entity to response DTO, and \
                    inject the service into the controller. A read-only service method that only \
                    delegates to findById is fine: the value is the boundary, where transactions, \
                    authorisation and the response contract will live. A handler that writes more than \
                    once makes one call to one @Transactional service method.""")
            .howNotToFix("""
                    Do NOT rename the repository or the controller: the rule matches the @Repository \
                    stereotype, Spring Data's Repository interface and @RepositoryDefinition, not \
                    names. Do NOT narrow the injection point to a fragment interface the repository \
                    also implements — QueryByExampleExecutor, QuerydslPredicateExecutor, \
                    JpaSpecificationExecutor or a project's own custom fragment: it carries none of \
                    those markers, so this rule does not see it, and it is still the repository. Do \
                    NOT swap the \
                    repository for EntityManager, JdbcTemplate, JdbcClient or another persistence API, \
                    or change a DAO's @Repository to @Component: this rule does not see either, and the \
                    controller still writes with no transaction around it. Do NOT wrap the repository in \
                    a class that forwards each call one-for-one while the controller still makes each \
                    write itself: the build passes, and each write still commits on its own. Do NOT \
                    move the repository into an unannotated base class the controller extends, or into \
                    a nested or anonymous class inside it: this rule checks only the annotated class's \
                    own code, so it does not see a repository that only those classes use. Do NOT look \
                    the repository up through ApplicationContext.getBean(...) or Spring Data's \
                    Repositories: a lookup by class literal is still reported, and so is every \
                    repository method the controller calls on the result; a lookup by bean name whose \
                    result is only cast and passed on is not, and it hides the dependency instead of \
                    removing it. Do NOT try to move the class out of scope by \
                    naming it like a test: scope is decided by output location and declared test \
                    methods.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .that(TestScope.PRODUCTION_CLASSES)
            .and().areMetaAnnotatedWith("org.springframework.stereotype.Controller")
            .should().dependOnClassesThat().areMetaAnnotatedWith("org.springframework.stereotype.Repository")
            .orShould().dependOnClassesThat().areAssignableTo("org.springframework.data.repository.Repository")
            .orShould().dependOnClassesThat().areMetaAnnotatedWith("org.springframework.data.repository.RepositoryDefinition");

    @ArchTest
    public static final ArchRule rule = new NoRepositoryInControllerRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
