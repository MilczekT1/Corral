package io.github.milczekt1.corral.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.corral.rules.jakarta.nojavaxservlet.NoJavaxServletRule;
import lombok.experimental.UtilityClass;

/**
 * Rules for valid use of the Jakarta EE APIs: code that compiles and starts, then silently misbehaves.
 */
@UtilityClass
public class JakartaRulesGroup {

    @ArchTest
    public static final ArchTests noJavaxServlet = ArchTests.in(NoJavaxServletRule.class);
}
