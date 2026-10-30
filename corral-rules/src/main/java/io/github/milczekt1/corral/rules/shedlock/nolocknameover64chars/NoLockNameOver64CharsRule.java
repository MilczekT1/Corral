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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No method or type may carry a ShedLock {@code @SchedulerLock} whose literal {@code name} is longer
 * than 64 characters, the width of the {@code name} column in ShedLock's documented SQL schema.
 *
 * <p>A name containing {@code ${} or {@code #{} resolves at runtime and is skipped. A method using a
 * composed annotation is not flagged; the composed annotation type is.
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
                    Do NOT move the long name into a ${...} property or a #{...} expression: this \
                    rule skips such names, and the resolved name still fails at the database — this \
                    rule does NOT catch it. Do NOT cut the name at character 64: two jobs whose names \
                    share a long prefix then share one lock and one silently skips its runs — not \
                    caught. Do NOT move the long name into a composed annotation's @AliasFor \
                    attribute, or into a LockConfiguration passed to LockingTaskExecutor — this rule \
                    catches neither. Do NOT remove @SchedulerLock: the job then runs on every node at \
                    once — not caught. Do NOT freeze or exclude a violation while the column is 64 \
                    characters wide: it is not debt, the job is not running.""")
            .build();

    /** Matched by name: each lives in its own artifact, and most consumers carry at most one. */
    static final List<String> SCHEDULER_LOCKS = List.of(
            "net.javacrumbs.shedlock.spring.annotation.SchedulerLock",
            "net.javacrumbs.shedlock.core.SchedulerLock",
            "net.javacrumbs.shedlock.cdi.SchedulerLock",
            "net.javacrumbs.shedlock.micronaut.SchedulerLock");

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
                if (hasOverLongLockName(javaClass)) {
                    events.add(SimpleConditionEvent.violated(javaClass, "Class <" + javaClass.getName()
                            + "> has a @SchedulerLock name longer than 64 characters in "
                            + javaClass.getSourceCodeLocation()));
                }
                for (JavaMethod method : javaClass.getMethods()) {
                    if (hasOverLongLockName(method)) {
                        events.add(SimpleConditionEvent.violated(method, "Method <" + method.getFullName()
                                + "> has a @SchedulerLock name longer than 64 characters in "
                                + method.getSourceCodeLocation()));
                    }
                }
            }
        };
    }

    /**
     * Do not drop the {@code ${} and {@code #{} skips: ShedLock resolves placeholders and SpEL in the
     * name, so the literal's length says nothing about the stored one.
     */
    private static boolean hasOverLongLockName(HasAnnotations<?> annotated) {
        return SCHEDULER_LOCKS.stream()
                .flatMap(type -> annotated.tryGetAnnotationOfType(type).stream())
                .flatMap(annotation -> annotation.get("name").stream())
                .map(String.class::cast)
                .anyMatch(name -> !name.contains("${") && !name.contains("#{")
                        && name.codePointCount(0, name.length()) > MAX_NAME_LENGTH);
    }
}
