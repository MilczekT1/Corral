package io.github.milczekt1.corral.rules.shedlock.nolocknameover64chars;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No method or type may carry a ShedLock {@code @SchedulerLock} whose literal {@code name} is longer
 * than 64 characters, the width of the {@code name} column in ShedLock's documented SQL schema.
 *
 * <p>For the Spring, Micronaut and {@code core} annotations, only the text outside any {@code ${...}}
 * placeholder or {@code #{...}} template is counted, since those resolve at runtime. ShedLock's CDI
 * integration resolves neither and reads the annotation only from the method, so a CDI name is counted
 * whole and only on a method. A method using a composed annotation is not flagged; the composed
 * annotation type is.
 *
 * @see <a href="https://github.com/lukas-krecan/ShedLock#jdbctemplate">ShedLock: the lock table schema</a>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoLockNameOver64CharsRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.shedlock.no-lock-name-over-64-chars")
            .why("""
                    ShedLock's documented schema declares name VARCHAR(64) NOT NULL as the primary key \
                    of the shedlock table, and the SQL-backed lock providers insert the lock name into \
                    that column the first time a job takes its lock. A longer name fails that insert \
                    ("value too long"), JdbcTemplateLockProvider wraps the error in a LockException, \
                    and the scheduled method fails at every trigger and never runs. Nothing else \
                    fails: the application starts, health checks stay green, and the only sign is an \
                    error in the log on every tick. Tests rarely let a scheduled job fire against the \
                    real lock table, so the first real insert happens in a deployed environment.""")
            .howToFix("""
                    Shorten the name to 64 characters or fewer; a short <service>.<job> name such as \
                    billing.overdue-reminders is enough. Keep it unique among all jobs that share the \
                    lock table, including jobs in other services using the same database: two jobs \
                    with one name share one lock and block each other. Keep lock names ASCII: this \
                    rule counts characters, but Oracle sizes VARCHAR(64) in bytes by default, so a \
                    non-ASCII name can exceed the column with fewer than 64 characters. If the \
                    project really needs longer names, widen the name column with a migration in \
                    every environment first, then exclude this rule in corral.properties with that \
                    reason. A project whose lock provider is not SQL-backed (MongoDB, Redis, \
                    ZooKeeper, Hazelcast and others) has no such limit and should not wire this \
                    rule.""")
            .howNotToFix("""
                    Do NOT move the long name, or part of it, into a ${...} property or a #{...} \
                    expression: on the Spring, Micronaut and core annotations this rule counts only \
                    the text outside them, and the resolved name still fails at the database — this \
                    rule does NOT catch it. Do NOT cut the name at character 64: two jobs whose names \
                    share a long prefix then share one lock and one silently skips its runs — not \
                    caught. Do NOT move the long name into a composed annotation's @AliasFor \
                    attribute, or into a LockConfiguration passed to LockingTaskExecutor — this rule \
                    catches neither. Do NOT move a CDI @SchedulerLock from the method to the class: \
                    ShedLock's CDI integration reads it only from the method, so the job runs \
                    unlocked — not caught. Do NOT remove @SchedulerLock: the job then runs on every \
                    node at once — not caught. Do NOT freeze or exclude a violation while the column \
                    is 64 characters wide: it is not debt, the job is not running.""")
            .build();

    /** Resolve {@code ${...}} and {@code #{...}} in the name, and are read from a type as well as a method. */
    static final List<String> RESOLVING_LOCKS = List.of(
            "net.javacrumbs.shedlock.spring.annotation.SchedulerLock",
            "net.javacrumbs.shedlock.core.SchedulerLock",
            "net.javacrumbs.shedlock.micronaut.SchedulerLock");

    /** Takes the name as written, and is read only from a method. */
    static final String CDI_LOCK = "net.javacrumbs.shedlock.cdi.SchedulerLock";

    static final int MAX_NAME_LENGTH = 64;

    static final ArchRule DEFINITION = classes().should(notCarryAnOverLongLockName());

    @ArchTest
    public static final ArchRule rule = new NoLockNameOver64CharsRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }

    private static ArchCondition<JavaClass> notCarryAnOverLongLockName() {
        return new ArchCondition<>(
                "not carry a ShedLock @SchedulerLock whose literal name is longer than 64 characters") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (hasOverLongResolvingName(javaClass)) {
                    events.add(SimpleConditionEvent.violated(javaClass, "Class <" + javaClass.getName()
                            + "> has a @SchedulerLock name longer than 64 characters in "
                            + javaClass.getSourceCodeLocation()));
                }
                for (JavaMethod method : javaClass.getMethods()) {
                    if (hasOverLongResolvingName(method) || hasOverLongCdiName(method)) {
                        events.add(SimpleConditionEvent.violated(method, "Method <" + method.getFullName()
                                + "> has a @SchedulerLock name longer than 64 characters in "
                                + method.getSourceCodeLocation()));
                    }
                }
            }
        };
    }

    private static boolean hasOverLongResolvingName(HasAnnotations<?> annotated) {
        return RESOLVING_LOCKS.stream()
                .flatMap(type -> lockNames(annotated, type))
                .anyMatch(name -> isOverLong(literalText(name)));
    }

    private static boolean hasOverLongCdiName(JavaMethod method) {
        return lockNames(method, CDI_LOCK).anyMatch(NoLockNameOver64CharsRule::isOverLong);
    }

    private static Stream<String> lockNames(HasAnnotations<?> annotated, String annotationType) {
        return annotated.tryGetAnnotationOfType(annotationType).stream()
                .flatMap(annotation -> annotation.get("name").stream())
                .map(String.class::cast);
    }

    private static boolean isOverLong(String name) {
        return name.codePointCount(0, name.length()) > MAX_NAME_LENGTH;
    }

    /**
     * The name without its {@code ${...}} and {@code #{...}} parts, braces nested inside them included.
     * Resolution keeps this text as written, so its length is a lower bound on the stored name's.
     */
    private static String literalText(String name) {
        StringBuilder literal = new StringBuilder();
        int depth = 0;
        int i = 0;
        while (i < name.length()) {
            if (depth == 0 && (name.startsWith("${", i) || name.startsWith("#{", i))) {
                depth = 1;
                i += 2;
            } else {
                char c = name.charAt(i);
                if (depth == 0) {
                    literal.append(c);
                } else if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                }
                i++;
            }
        }
        return literal.toString();
    }
}
