package io.github.milczekt1.corral.rules.jakarta.nojavaxservlet;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may depend on anything in {@code javax.servlet..}.
 *
 * <p>Matched by package string, so a consumer without the legacy API on the classpath still
 * compiles and runs the rule.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoJavaxServletRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.jakarta.no-javax-servlet")
            .why("""
                    javax.servlet.Filter and jakarta.servlet.Filter are unrelated types, and Tomcat 10+, \
                    Jetty 11+ and Undertow 2.3+ load only jakarta.servlet. A filter or servlet written \
                    against javax.servlet compiles and its bean is created, then registration fails at \
                    startup with a ClassCastException or NoClassDefFoundError: javax/servlet/Filter, in a \
                    stack trace that points at Spring's registration code rather than at the class. When \
                    the javax API is still on the classpath it does not fail at all: the filter is \
                    registered as a type the container never consults, and a HandlerInterceptor or \
                    controller method taking javax.servlet.http.HttpServletRequest is never matched. A \
                    security filter that does not run is an authentication check that does not happen.""")
            .howToFix("""
                    Rewrite the import to jakarta.servlet and move the whole class at once, including \
                    HttpServletRequest and HttpServletResponse parameters, ServletException, FilterChain, \
                    and any FilterRegistrationBean or ServletRegistrationBean type arguments. Then check \
                    that the container is Servlet 5.0 or later (Tomcat 10, Jetty 11, Spring Boot 3) and \
                    that javax.servlet:javax.servlet-api is gone from the build — find what brings it in \
                    with mvn dependency:tree. A third-party library that still depends on javax.servlet \
                    needs upgrading: a Jakarta container will not load it either. OpenRewrite's \
                    org.openrewrite.java.migrate.jakarta.JavaxServletToJakartaServlet recipe does the \
                    mechanical rewrite.""")
            .howNotToFix("""
                    Do NOT add the javax.servlet API jar back to the runtime classpath to clear the \
                    NoClassDefFoundError: the container still will not invoke the filter, so a startup \
                    failure becomes a security control that silently does nothing. Do NOT run a \
                    bytecode-rewriting shim such as the Tomcat jakartaee-migration tool over your own \
                    classes instead of migrating the source — that is for third-party jars you cannot \
                    rebuild. Do NOT swap the flagged type for a different javax.servlet one: the whole \
                    package, including javax.servlet.http and javax.servlet.annotation, is matched. Not \
                    caught: filters, servlets and listeners declared in web.xml, which is not bytecode.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .should().dependOnClassesThat().resideInAPackage("javax.servlet..");

    @ArchTest
    public static final ArchRule rule = new NoJavaxServletRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
