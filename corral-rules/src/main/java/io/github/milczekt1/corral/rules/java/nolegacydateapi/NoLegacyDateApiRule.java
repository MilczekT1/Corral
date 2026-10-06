package io.github.milczekt1.corral.rules.java.nolegacydateapi;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.corral.DocumentedRule;
import io.github.milczekt1.corral.doc.RuleDoc;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * No class may depend on a type assignable to {@code java.util.Date}, {@code java.util.Calendar},
 * {@code java.text.DateFormat} or {@code java.util.TimeZone}.
 *
 * <p>Stands alone, in no group: wire it with {@code ArchTests.in(NoLegacyDateApiRule.class)}. A local
 * variable is not a bytecode dependency, so a {@code Timestamp} read into one and never called,
 * returned or stored in a field is not seen.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoLegacyDateApiRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("corral.java.no-legacy-date-api")
            .why("""
                    Date, Calendar, DateFormat and TimeZone are mutable, and none of them is \
                    thread-safe. A Date returned from a getter is a live reference: a caller calling \
                    setTime on it silently edits your object. Calendar.MONTH is zero-based, so December \
                    is 11 and the off-by-one ships as a report dated a month early. SimpleDateFormat \
                    keeps parse state in an instance field, so a shared instance formatting two dates \
                    at once returns one of them twice or throws from inside DecimalFormat. \
                    TimeZone.getTimeZone returns GMT rather than failing for an id it does not \
                    recognise. And Date is not a date: it is an instant with no zone, printed in the \
                    JVM's default zone, so the same object reads as two different days on a laptop and \
                    on a UTC container. java.time makes code say which it meant — Instant for a moment, \
                    LocalDate for a calendar day, ZonedDateTime when the zone is part of the meaning.""")
            .howToFix("""
                    Replace the type at the boundary, not just at the call site. Date becomes Instant \
                    when it is a timestamp and LocalDate when it is a day; Calendar arithmetic becomes \
                    plusDays/plusMonths on the java.time type; SimpleDateFormat becomes a static final \
                    DateTimeFormatter, which is immutable and safe to share; TimeZone becomes ZoneId. \
                    The database column does not change, only the Java type reading it: in JDBC or a \
                    Spring RowMapper replace getTimestamp/setTimestamp with \
                    rs.getObject(i, LocalDateTime.class) / ps.setObject(i, value) — a JDBC 4.2 driver \
                    maps LocalDate, LocalTime, LocalDateTime, OffsetTime and OffsetDateTime directly. \
                    In a JPA entity type the field as one of those and drop @Temporal; JPA 2.2+ maps \
                    them without an AttributeConverter. Instant is mapped by Hibernate and by JPA 3.2+, \
                    but not by the JDBC 4.2 or JPA 2.2 specifications. At an API edge you cannot \
                    change — a third-party signature, a Jackson serializer, a JPA AttributeConverter \
                    for a legacy column — convert once with Date.from(instant) / date.toInstant() in \
                    one adapter class, keep the legacy type out of the domain, and exclude that \
                    adapter's package with a reason naming the API that forces the legacy type.""")
            .howNotToFix("""
                    Do NOT wrap the legacy type in a DateUtils helper and call that instead: the \
                    helper depends on it and is flagged, and hiding a dependency behind one class does \
                    not remove it. Do NOT swap SimpleDateFormat for DateFormat.getInstance(), or \
                    java.util.Date for java.sql.Timestamp, java.sql.Date or java.sql.Time: they are \
                    subtypes of the same four types, matched by the same check, with the same \
                    problems. Do NOT make a Date field final and call it immutable: final stops \
                    reassignment, not setTime. Do NOT change the database column type to avoid \
                    Timestamp; the column was never the problem.""")
            .build();

    static final ArchRule DEFINITION = noClasses()
            .should().dependOnClassesThat(
                    assignableTo(Date.class)
                            .or(assignableTo(Calendar.class))
                            .or(assignableTo(DateFormat.class))
                            .or(assignableTo(TimeZone.class)));

    @ArchTest
    public static final ArchRule rule = new NoLegacyDateApiRule().guard();

    @Override
    public ArchRule definition() {
        return DEFINITION;
    }

    @Override
    public RuleDoc doc() {
        return DOC;
    }
}
