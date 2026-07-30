# Lombok Adoption and DatabaseRules Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adopt Lombok inside the `rules` module with zero call-site changes, guard against Lombok silently generating nothing, and remove every trace left behind by the `DatabaseRules` removal — including restoring the example consumer's freeze-store demonstration.

**Architecture:** Lombok is wired as an annotation processor via `annotationProcessorPaths` (not merely a `provided` dependency, which is inert on JDK 23+). Three utility classes lose their hand-written private constructors to `@UtilityClass`; the failure formatter loses its hand-written logger to `@Slf4j`; `RuleDoc` loses ~27 lines of builder to `@Builder`, keeping a small partial builder that preserves both the `String` ergonomics and the `Optional.empty()` default. A guard test anchors on a member only Lombok can produce, so a future misconfiguration fails loudly instead of shipping a broken jar.

**Tech Stack:** Java 25, Maven, Lombok 1.18.46, ArchUnit 1.4.2, JUnit Jupiter 5.11.3.

**Source spec:** `docs/superpowers/specs/2026-07-30-lombok-adoption-and-cleanup-design.md`

---

## Verified facts (spikes already run — do not re-litigate)

Confirmed against Lombok 1.18.46 on JDK 25 before this plan was written.

1. **Lombok 1.18.46 works on JDK 25.** The blocker is javac, not Lombok: JDK 23+ disabled implicitly-enabled annotation processing from the classpath. With Lombok only as a `provided` dependency, `mvn verify` reports **BUILD SUCCESS** while `@Getter` generates nothing. `annotationProcessorPaths` fixes it. The target `--release` is irrelevant — `--release 21` under JDK 25 is still inert.
2. **`@Builder` works on a record**, and compact-constructor validation is preserved (the generated builder calls the canonical constructor). A malformed id still throws.
3. **`@Builder` on a record generates `howNotToFix(Optional<String>)`**, not `(String)`.
4. **An omitted `Optional` component arrives as `null`, not `Optional.empty()`.** Without a hand-written field initializer, `RuleDoc.builder().id(..).why(..).howToFix(..).build()` throws `howNotToFix must not be null`. Existing docs and tests do exactly that. **This is the most breakage-prone detail in this plan.**
5. **Lombok suppresses generation by method *name*, not signature.** Hand-writing `howNotToFix(String)` means no `howNotToFix(Optional<String>)` overload is generated at all. Verified generated `Builder` members: `id`, `why`, `howToFix`, the hand-written `howNotToFix(String)`, `build()`, `toString()`.
6. **`@Builder(builderClassName = "Builder")` preserves the `RuleDoc.Builder` type name.** Without it Lombok names it `RuleDocBuilder`, breaking `RuleDocTest:12`'s `private static RuleDoc.Builder valid()`.
7. **`@UtilityClass` rejects an already-declared constructor**, failing loudly: `@UtilityClasses cannot have declared constructors.`
8. **`@UtilityClass` output matches the current hand-written shape**: `public final class`, `private` constructor, static members.
9. **A hand-written partial builder class is merged, not replaced.**

---

## Global Constraints

- `maven.compiler.release` stays **25**. Do not change it.
- **No rule id changes.** Ids are freeze-store keys; changing one orphans consumers' frozen violations.
- **Zero call-site changes.** The existing 49 `rules` tests must pass unchanged. If a test needs editing, the approach has failed — report it rather than editing the test.
- Lombok at **`provided`** scope only, so it never reaches consumers.
- `AllCentralRules` and `TestingRules` are **not** annotated. `@UtilityClass` rewrites member modifiers and ArchUnit reflects over their `@ArchTest` fields.
- No new rule groups. Lombok is used *by* the library, not governed *for* consumers.
- Module layout is `rules/` and `rules-example/` (renamed from `llama-rules/` and `examples/consumer-junit5/`).

---

## File Structure

```text
pom.xml                                    # MODIFY: lombok.version, dependencyManagement, annotationProcessorPaths
rules/pom.xml                              # MODIFY: lombok provided dependency
rules/src/main/java/io/github/milczekt1/archrules/
  RuleRegistry.java                        # MODIFY: @UtilityClass, drop private ctor
  FrozenRules.java                         # MODIFY: @UtilityClass, drop private ctor
  RuleDoc.java                             # MODIFY: @Builder, drop hand-written builder body
  format/
    AntiFixPolicy.java                     # MODIFY: @UtilityClass, drop private ctor
    AgentFriendlyFailureDisplayFormat.java # MODIFY: @Slf4j, drop manual Logger field
rules/src/test/java/io/github/milczekt1/archrules/
  LombokWiringTest.java                    # CREATE: guard against silent no-op code generation
  fixtures/database/**                     # DELETE: 6 orphaned files
rules-example/src/main/java/com/example/consumer/
  service/ReportService.java               # DELETE: existed only for the raw-JDBC rule
  repository/CustomerRepository.java        # DELETE: same
rules-example/src/test/java/com/example/consumer/
  LegacyChecks.java                        # CREATE: seeds the freeze store, zero new dependencies
rules-example/src/test/resources/archunit/frozen/   # CREATE: re-seeded, committed
README.md                                  # MODIFY: stale DatabaseRules reference, rules table
```

**Decomposition rationale:** Task 1 owns the build wiring plus the one Lombok feature whose failure is genuinely *silent* (`@UtilityClass`), so the guard test has something real to guard. Tasks 2 and 3 are independent single-class migrations a reviewer could accept or reject separately. Tasks 4 and 5 are the cleanup half and touch no library production code.

---

## Task 1: Lombok wiring, `@UtilityClass`, and the silent-failure guard

**Files:**
- Modify: `pom.xml`, `rules/pom.xml`
- Modify: `rules/src/main/java/io/github/milczekt1/archrules/RuleRegistry.java`, `FrozenRules.java`
- Modify: `rules/src/main/java/io/github/milczekt1/archrules/format/AntiFixPolicy.java`
- Test: `rules/src/test/java/io/github/milczekt1/archrules/LombokWiringTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: a working Lombok toolchain for the module. `RuleRegistry`, `FrozenRules` and `AntiFixPolicy` keep their exact public API (`register`/`find`/`all`, `freeze`, `clauses`/`addClause`) and stay non-instantiable. Tasks 2 and 3 depend on the build wiring.

> **Why this task carries the guard.** `@UtilityClass` is the one annotation here whose absence is
> silent: delete the private constructor, lose Lombok, and Java supplies an implicit **public**
> constructor. The class still compiles, tests still pass, and the library ships an instantiable
> "utility" class. `@Builder` and `@Slf4j` fail loudly instead, because their generated members are
> referenced in the same module. So the guard belongs here.

- [ ] **Step 1: Write the failing test**

Create `rules/src/test/java/io/github/milczekt1/archrules/LombokWiringTest.java`:

```java
package io.github.milczekt1.archrules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.archrules.format.AntiFixPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards against Lombok silently generating nothing.
 *
 * <p>When annotation processing is misconfigured, Lombok produces no code and the build still
 * reports SUCCESS. For {@code @UtilityClass} specifically the damage is invisible: the hand-written
 * private constructor has been deleted, so Java supplies an implicit <em>public</em> one and the
 * class becomes instantiable. Nothing else in the build would notice.
 *
 * <p>These assertions therefore anchor on the shape {@code @UtilityClass} produces, which only
 * Lombok can produce now that the explicit constructors are gone.
 */
class LombokWiringTest {

    private static final List<Class<?>> UTILITY_CLASSES =
            List.of(RuleRegistry.class, FrozenRules.class, AntiFixPolicy.class);

    @Test
    void utilityClassesAreFinal() {
        for (Class<?> type : UTILITY_CLASSES) {
            assertTrue(Modifier.isFinal(type.getModifiers()),
                    type.getSimpleName() + " must be final — @UtilityClass did not run");
        }
    }

    @Test
    void utilityClassesCannotBeInstantiated() {
        for (Class<?> type : UTILITY_CLASSES) {
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            assertEquals(1, constructors.length,
                    type.getSimpleName() + " should declare exactly one constructor");
            assertTrue(Modifier.isPrivate(constructors[0].getModifiers()),
                    type.getSimpleName() + " has a non-private constructor — @UtilityClass did not run,"
                            + " so Java supplied an implicit public one");
        }
    }

    @Test
    void utilityClassMembersRemainStatic() {
        for (Class<?> type : UTILITY_CLASSES) {
            for (var method : type.getDeclaredMethods()) {
                if (method.isSynthetic()) {
                    continue;
                }
                assertTrue(Modifier.isStatic(method.getModifiers()),
                        type.getSimpleName() + "." + method.getName() + " must be static");
            }
        }
    }
}
```

- [ ] **Step 2: Delete the private constructors, then run the test to watch it fail**

This is the RED step and it must be done in this order — the test only fails once the constructors are gone and Lombok is not yet wired.

Delete from `RuleRegistry.java`:

```java
    private RuleRegistry() {
    }
```

Delete from `FrozenRules.java`:

```java
    private FrozenRules() {
    }
```

Delete from `AntiFixPolicy.java`:

```java
    private AntiFixPolicy() {
    }
```

Run: `./mvnw -B -q -pl rules test -Dtest=LombokWiringTest`

Expected: FAIL — specifically `utilityClassesCannotBeInstantiated`, reporting a non-private constructor because Java supplied an implicit public one. `utilityClassesAreFinal` and `utilityClassMembersRemainStatic` will already **pass** at this point, since the classes are declared `final` and their members `static` by hand today; only the constructor assertion discriminates. That is expected — do not "fix" the other two to fail.

Capture the failure output verbatim. It is the exact damage this guard exists to detect.

- [ ] **Step 3: Wire Lombok into the build**

In the root `pom.xml`, add to `<properties>`:

```xml
    <lombok.version>1.18.46</lombok.version>
```

Add to `<dependencyManagement><dependencies>`:

```xml
      <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <scope>provided</scope>
      </dependency>
```

In the root `pom.xml` `<pluginManagement>`, replace the `maven-compiler-plugin` entry with:

```xml
        <!-- Pinned deliberately: Maven 3.8.x defaults to compiler plugin 3.1,
             which ignores maven.compiler.release and fails with "Source option 5". -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.13.0</version>
          <configuration>
            <!-- MANDATORY, not decorative. JDK 23+ switched off implicitly-enabled annotation
                 processing from the classpath. Without this path Lombok never runs, generates
                 nothing, and the build still reports SUCCESS — see LombokWiringTest. -->
            <annotationProcessorPaths>
              <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
              </path>
            </annotationProcessorPaths>
          </configuration>
        </plugin>
```

In `rules/pom.xml`, add as the first dependency:

```xml
    <!-- Compile-time only: an annotation processor. `provided` keeps it off every consumer's
         classpath. The processor itself is wired via annotationProcessorPaths in the parent. -->
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <scope>provided</scope>
    </dependency>
```

- [ ] **Step 4: Add `@UtilityClass` to the three classes**

`RuleRegistry.java` — add the import and the annotation:

```java
import lombok.experimental.UtilityClass;
```

```java
@UtilityClass
public class RuleRegistry {
```

Note the `final` keyword is dropped from the class declaration: `@UtilityClass` makes the class final itself, and leaving an explicit `final` is redundant. The `static` keywords on existing members may stay — `@UtilityClass` makes members static implicitly, and leaving them explicit keeps the code readable to someone who does not know Lombok.

Apply the same two edits to `FrozenRules.java` and `format/AntiFixPolicy.java` (`public final class X` becomes `@UtilityClass` + `public class X`).

- [ ] **Step 5: Run the test to verify it passes**

Run: `./mvnw -B -q -pl rules test -Dtest=LombokWiringTest`

Expected: PASS — 3 tests, 0 failures.

- [ ] **Step 6: Run the full module**

Run: `./mvnw -B -q -pl rules test`

Expected: PASS — 52 tests (49 existing + 3 new), 0 failures. If any pre-existing test changed behaviour, stop and report: the "zero call-site change" constraint has been violated.

- [ ] **Step 7: Prove the guard actually guards**

Temporarily comment out the `<annotationProcessorPaths>` block in the root pom, then:

Run: `./mvnw -B -q -pl rules test -Dtest=LombokWiringTest`

Expected: FAIL on the non-private constructor. Restore the block, re-run, confirm green. Paste both outputs into your report — a guard that has never been observed failing is not known to guard anything.

- [ ] **Step 8: Tell contributors they need the IDE plugin**

Lombok is a compile-time processor: without the IDE plugin, IntelliJ and Eclipse report phantom
errors on generated members even though the Maven build is green. Add a short section to
`README.md`, immediately above the existing `## License` heading:

```markdown
## Contributing

This module uses [Lombok](https://projectlombok.org/). Install your IDE's Lombok plugin, or the IDE
will report errors on generated members that `mvn verify` compiles cleanly.

Lombok is wired through `annotationProcessorPaths` in the root `pom.xml`, not just as a dependency —
JDK 23+ ignores annotation processors that are only on the classpath, and would otherwise generate
nothing while still reporting a successful build. `LombokWiringTest` guards against that.
```

- [ ] **Step 9: Commit**

```bash
git add pom.xml rules/pom.xml README.md \
        rules/src/main/java/io/github/milczekt1/archrules/RuleRegistry.java \
        rules/src/main/java/io/github/milczekt1/archrules/FrozenRules.java \
        rules/src/main/java/io/github/milczekt1/archrules/format/AntiFixPolicy.java \
        rules/src/test/java/io/github/milczekt1/archrules/LombokWiringTest.java
git commit -m "build: wire Lombok as an annotation processor and adopt @UtilityClass"
```

---

## Task 2: `@Slf4j` on the failure formatter

**Files:**
- Modify: `rules/src/main/java/io/github/milczekt1/archrules/format/AgentFriendlyFailureDisplayFormat.java`
- Test: no new test — covered by Task 1's guard plus the existing 11 formatter tests

**Interfaces:**
- Consumes: Lombok wiring from Task 1.
- Produces: identical behaviour. The `log` field keeps the same name, type and target class, so the `debug(...)` helper and all six call sites are untouched.

> **The comment matters more than the two lines saved.** The current `Logger` field carries a
> javadoc explaining why this library depends on `slf4j-api` but ships **no binding** — a real
> design decision a future contributor could otherwise undo. `@Slf4j` deletes the field and its
> javadoc with it. Move that prose to the class javadoc rather than losing it.

- [ ] **Step 1: Apply the change**

In `AgentFriendlyFailureDisplayFormat.java`, delete these imports:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

Delete the field and its javadoc:

```java
    /**
     * {@code slf4j-api} only — it is already on the compile classpath transitively via ArchUnit, and
     * this library deliberately ships no binding, so a consumer's own logging setup decides whether
     * these messages surface. Every degradation below is logged at DEBUG: silently falling back is
     * what made this class undiagnosable in a consumer's CI.
     */
    private static final Logger log = LoggerFactory.getLogger(AgentFriendlyFailureDisplayFormat.class);
```

Add the import and annotation:

```java
import lombok.extern.slf4j.Slf4j;
```

```java
@Slf4j
public class AgentFriendlyFailureDisplayFormat implements FailureDisplayFormat {
```

Append this paragraph to the **class** javadoc, immediately before the closing `*/`, so the rationale survives:

```java
 * <p>Logging uses {@code slf4j-api} only — already on the compile classpath transitively via
 * ArchUnit — and this library deliberately ships no binding, so a consumer's own logging setup
 * decides whether these messages surface. Every degradation below is logged at DEBUG: silently
 * falling back is what made this class undiagnosable in a consumer's CI.
```

- [ ] **Step 2: Verify the generated field is equivalent**

Run: `./mvnw -B -q -pl rules test-compile && javap -p -cp rules/target/classes io.github.milczekt1.archrules.format.AgentFriendlyFailureDisplayFormat | grep -i logger`

Expected: `private static final org.slf4j.Logger log;` — same name, same type as the deleted field.

If the field is absent, Lombok did not run. Do not work around it; the Task 1 wiring is broken.

- [ ] **Step 3: Run the tests**

Run: `./mvnw -B -q -pl rules test -Dtest=AgentFriendlyFailureDisplayFormatTest`

Expected: PASS — 11 tests, 0 failures, unchanged.

Then the full module: `./mvnw -B -q -pl rules test` — 52 tests, 0 failures.

- [ ] **Step 4: Commit**

```bash
git add rules/src/main/java/io/github/milczekt1/archrules/format/AgentFriendlyFailureDisplayFormat.java
git commit -m "refactor: replace hand-written logger with @Slf4j"
```

> **Known limitation, unchanged by this task.** A previously parked review finding notes that the
> static logger field initializer sits outside the class's never-throw guard, so a broken SLF4J
> provider would throw `ExceptionInInitializerError` before `formatFailure` is entered. `@Slf4j`
> generates the same static field, so this is neither fixed nor worsened here. Do not attempt to fix
> it in this task.

---

## Task 3: `@Builder` on `RuleDoc`

**Files:**
- Modify: `rules/src/main/java/io/github/milczekt1/archrules/RuleDoc.java`
- Test: no new test — `RuleDocTest`'s existing 5 tests are the acceptance criteria and must pass **unchanged**

**Interfaces:**
- Consumes: Lombok wiring from Task 1.
- Produces: `RuleDoc.builder()` returning `RuleDoc.Builder`, with `id(String)`, `why(String)`, `howToFix(String)`, `howNotToFix(String)` and `build()`. Identical to today's API. `RuleDocTest:12`'s `private static RuleDoc.Builder valid()` must keep compiling untouched.

> **Three things here will break the build if you get them wrong**, all verified by spike:
> 1. Without `builderClassName = "Builder"`, Lombok names the class `RuleDocBuilder` and
>    `RuleDoc.Builder` stops resolving.
> 2. Without the hand-written `howNotToFix(String)`, Lombok generates `howNotToFix(Optional<String>)`
>    and every call site must change.
> 3. Without the hand-written field **initializer**, an omitted `howNotToFix` arrives as `null` and
>    `build()` throws `howNotToFix must not be null`. Several existing docs and tests omit it.

- [ ] **Step 1: Apply the change**

In `RuleDoc.java`, add the import:

```java
import lombok.Builder;
```

Annotate the record:

```java
@Builder(builderClassName = "Builder")
public record RuleDoc(String id, String why, String howToFix, Optional<String> howNotToFix) {
```

Delete the `builder()` factory — Lombok generates it:

```java
    public static Builder builder() {
        return new Builder();
    }
```

Replace the entire hand-written `Builder` class with this partial one:

```java
    /**
     * Partially hand-written on purpose; Lombok generates the rest.
     *
     * <p>Two members are carried by hand because Lombok's defaults are wrong for this type:
     * the field initializer, because an omitted {@code Optional} component would otherwise arrive
     * as {@code null} and trip the canonical constructor's null check; and the {@code String}
     * setter, because the generated one would take {@code Optional<String>} and force every rule
     * author to write {@code .howNotToFix(Optional.of("..."))}. Lombok suppresses generation by
     * method <em>name</em>, so declaring the {@code String} form means no {@code Optional} overload
     * is generated at all.
     */
    public static class Builder {

        private Optional<String> howNotToFix = Optional.empty();

        public Builder howNotToFix(String howNotToFix) {
            this.howNotToFix = Optional.ofNullable(howNotToFix);
            return this;
        }
    }
```

Leave the compact constructor, `ID_PATTERN` and `requireText` exactly as they are. Note the class is `public static class`, not `public static final class` — Lombok extends it.

- [ ] **Step 2: Run `RuleDocTest` — it must pass without edits**

Run: `./mvnw -B -q -pl rules test -Dtest=RuleDocTest`

Expected: PASS — 5 tests, 0 failures, with `RuleDocTest.java` unmodified.

`howNotToFixIsOptionalAndDefaultsToEmpty` is the one that catches verified fact #4; `rejectsBlankRequiredFields` and `rejectsIdsThatWouldMakeUnstableOrUnreadableFreezeKeys` prove the compact constructor still validates through the generated builder.

**If any of these fail, do not edit the test.** The failure means the partial builder is wrong — recheck the field initializer and `builderClassName`.

- [ ] **Step 3: Verify the generated builder shape**

Run: `javap -p -cp rules/target/test-classes:rules/target/classes 'io.github.milczekt1.archrules.RuleDoc$Builder'`

Expected members: `id`, `why`, `howToFix`, `howNotToFix(java.lang.String)`, `build()`. There must be **no** `howNotToFix(java.util.Optional)` overload — its presence would mean the hand-written method was named differently and Lombok generated its own alongside.

- [ ] **Step 4: Run the full module**

Run: `./mvnw -B -q -pl rules test`

Expected: PASS — 52 tests, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add rules/src/main/java/io/github/milczekt1/archrules/RuleDoc.java
git commit -m "refactor: generate RuleDoc's builder with Lombok, preserving its API"
```

---

## Task 4: Remove the `DatabaseRules` debris

**Files:**
- Delete: `rules/src/test/java/io/github/milczekt1/archrules/fixtures/database/` (whole tree, 6 files)
- Delete: `rules-example/src/main/java/com/example/consumer/service/ReportService.java`
- Delete: `rules-example/src/main/java/com/example/consumer/repository/CustomerRepository.java`
- Modify: `README.md` (line 68)
- Modify: `rules-example/src/test/java/com/example/consumer/CentralArchitectureTest.java` (line 24 comment)

**Interfaces:**
- Consumes: nothing from Tasks 1–3; this task is independent of the Lombok work.
- Produces: a tree with no reference to `DatabaseRules` or any `db.no-*` id outside the historical spec and plan documents, which are records of past decisions and must not be edited.

> **Nothing here should change behaviour.** Every file removed is already unreachable: the fixtures
> lost their only consumers when `DatabaseRules`, `DatabaseRulesTest`, `DatabaseRulesFrozenFieldsTest`
> and `FreezingBehaviourTest` were deleted, and the two example classes existed solely to violate
> the raw-JDBC rule. If deleting any of them breaks a test, stop and report — it means something
> still depends on it and this task's premise is wrong.

- [ ] **Step 1: Confirm the fixtures really are orphaned before deleting**

Run:

```bash
grep -rn "fixtures.database" rules/src rules-example/src || echo "no references"
```

Expected: `no references`. If anything is printed, stop and report rather than deleting.

- [ ] **Step 2: Delete the orphaned files**

```bash
rm -rf rules/src/test/java/io/github/milczekt1/archrules/fixtures/database
rm -f rules-example/src/main/java/com/example/consumer/service/ReportService.java
rm -f rules-example/src/main/java/com/example/consumer/repository/CustomerRepository.java
rmdir rules-example/src/main/java/com/example/consumer/repository 2>/dev/null || true
```

`GreetingService` stays — it is the example's benign production class and gives the scan something compliant to find.

- [ ] **Step 3: Fix the stale README reference**

`README.md:68` currently reads:

```markdown
Opt in group by group instead with `ArchTests.in(DatabaseRules.class)` / `ArchTests.in(TestingRules.class)`.
```

Replace with:

```markdown
Opt in group by group instead with `ArchTests.in(TestingRules.class)`.
```

Keep the sentence — group-by-group opt-in is still a real capability and the line stays correct as groups are added. Do not delete it.

- [ ] **Step 4: Fix the stale comment in the example**

`rules-example/src/test/java/com/example/consumer/CentralArchitectureTest.java:23-25` currently reads:

```java
    // Equivalent, if you prefer to opt in group by group:
    // @ArchTest static final ArchTests database = ArchTests.in(DatabaseRules.class);
    // @ArchTest static final ArchTests testing  = ArchTests.in(TestingRules.class);
```

Replace with:

```java
    // Equivalent, if you prefer to opt in group by group:
    // @ArchTest static final ArchTests testing = ArchTests.in(TestingRules.class);
```

- [ ] **Step 5: Verify nothing references the removed names**

Run:

```bash
grep -rn "DatabaseRules\|db\.no-" README.md rules/src rules-example/src || echo "clean"
```

Expected: `clean`. The `docs/superpowers/` spec and plan files legitimately still mention `DatabaseRules` as history — do not edit them.

- [ ] **Step 6: Run the full reactor**

Run: `./mvnw -B verify`

Expected: PASS. The `rules` module still reports 52 tests. The example module still reports 2.

- [ ] **Step 7: Commit**

```bash
git add -A README.md rules/src rules-example/src
git commit -m "chore: remove fixtures and example classes orphaned by the DatabaseRules removal"
```

---

## Task 5: Restore the example's freeze demonstration

**Files:**
- Create: `rules-example/src/test/java/com/example/consumer/LegacyChecks.java`
- Create: `rules-example/src/test/resources/archunit/frozen/` (generated by seeding, then committed)

**Interfaces:**
- Consumes: Task 4's cleanup (the example must have no leftover raw-JDBC violation).
- Produces: a committed freeze store proving that adoption absorbs pre-existing debt. This is the example's whole reason to exist beyond wiring.

> **Why the class name matters.** Surefire's default includes are `**/Test*.java`, `**/*Test.java`,
> `**/*Tests.java`, `**/*TestCase.java`. `LegacyChecks` matches none of them, so Surefire never runs
> it — which is exactly the bug `test.class-naming-convention` exists to catch, reproduced
> authentically. A name like `LegacyTestCase` would be executed as a real test and defeat the point.

- [ ] **Step 1: Create the deliberate violation**

Create `rules-example/src/test/java/com/example/consumer/LegacyChecks.java`:

```java
package com.example.consumer;

import org.junit.jupiter.api.Test;

/**
 * Deliberate, permanent violation of {@code test.class-naming-convention}.
 *
 * <p>It holds a JUnit test method but its name ends in neither {@code Test} nor {@code IT}, so no
 * build tool's default selection will ever run it — the exact failure that rule exists to catch.
 *
 * <p>It is left in place on purpose and frozen into the committed store, demonstrating the
 * library's central promise: adopting the rules records existing debt instead of blocking the
 * build. Only <em>new</em> violations fail.
 */
class LegacyChecks {

    @Test
    void checksSomethingNobodyRuns() {
        // Intentionally empty: this method's existence is the violation, not its body.
    }
}
```

JUnit Jupiter is already available transitively via `archunit-junit5`, so **no dependency change is needed** — that is the point of choosing this rule over the mocked-repository one.

- [ ] **Step 2: Confirm it fails the build before seeding**

Run: `./mvnw -B test -pl rules-example -am`

Expected: **FAIL** with `Architecture Violation [test.class-naming-convention]`, naming `LegacyChecks`, and carrying the `WHY:` / `HOW TO FIX:` / `HOW NOT TO FIX (always):` sections. Capture that message — it is the evidence that both the rule and the agent-friendly formatter work end to end through a real consumer.

If it instead fails with `StoreInitializationFailedException: Creating new violation store is disabled`, that is also correct at this point: it means the store does not exist yet and `allowStoreCreation` is properly not pinned. Proceed to Step 3.

- [ ] **Step 3: Seed the store**

Run:

```bash
./mvnw -B test -pl rules-example -am -Darchunit.freeze.store.default.allowStoreCreation=true
```

Expected: PASS. The violation is now recorded as pre-existing debt.

- [ ] **Step 4: Verify the store contents**

```bash
cat rules-example/src/test/resources/archunit/frozen/stored.rules
```

Expected: two entries keyed by **short rule ids**, not English sentences:

```
test.class-naming-convention=<uuid>
test.no-mocked-repository-in-integration-test=<uuid>
```

The `test.class-naming-convention` violation file must name `LegacyChecks`; the other rule's file must exist and be empty.

If the keys are English sentences rather than ids, `.as(doc.id())` is misapplied upstream — report it rather than working around it.

- [ ] **Step 5: Confirm the build is green without the seeding flag**

Run: `./mvnw -B verify`

Expected: PASS, with no `allowStoreCreation` override. This proves the committed store is doing its job.

- [ ] **Step 6: Prove a NEW violation still fails**

Temporarily add a second offender:

```bash
cat > rules-example/src/test/java/com/example/consumer/MoreLegacyChecks.java <<'EOF'
package com.example.consumer;

import org.junit.jupiter.api.Test;

class MoreLegacyChecks {
    @Test
    void alsoNeverRuns() {
    }
}
EOF
./mvnw -B test -pl rules-example -am
```

Expected: **FAIL**, naming `MoreLegacyChecks` but **not** `LegacyChecks` — the frozen one stays silent. Then:

```bash
rm rules-example/src/test/java/com/example/consumer/MoreLegacyChecks.java
git checkout -- rules-example/src/test/resources/archunit/frozen
./mvnw -B verify
```

Expected: PASS.

> The `git checkout` is not optional. `allowStoreUpdate` defaults to true, so the failing run may
> have rewritten the store, and **a store mutated by a failing run must never be committed.**

- [ ] **Step 7: Confirm `LegacyChecks` is not executed as a test**

```bash
ls rules-example/target/surefire-reports/ | grep -i legacy || echo "not executed - correct"
```

Expected: `not executed - correct`. If a report exists, Surefire is running it and the class needs renaming.

- [ ] **Step 8: Commit**

```bash
git add rules-example/src/test/java/com/example/consumer/LegacyChecks.java \
        rules-example/src/test/resources/archunit/frozen
git commit -m "docs: restore the example's freeze demonstration with a naming-rule violation"
```

---

## Final verification

Run from the repo root; all must pass before the work is considered done.

1. `./mvnw -B verify` — reactor green, `rules` 52 tests, `rules-example` 2 tests.
2. `./mvnw -B -pl rules test -Dsurefire.runOrder=reversealphabetical` — green. A previous review found an order-dependent test passing only by filesystem luck, so this is a standing requirement.
3. `javap -p -cp rules/target/classes io.github.milczekt1.archrules.RuleRegistry` — class `final`, constructor `private`, proving `@UtilityClass` ran.
4. `cat rules-example/src/test/resources/archunit/frozen/stored.rules` — short rule ids as keys.
5. `grep -rn "DatabaseRules\|db\.no-" README.md rules/src rules-example/src` — no output.
6. `git status --porcelain` — clean apart from the pre-existing untracked `central-arch-rules-framework-design.patch`.

## Out of scope

- New rule groups, including any group governing consumers' Lombok usage.
- Lowering `maven.compiler.release` below 25.
- `@UtilityClass` on `AllCentralRules` or `TestingRules`.
- The three parked robustness findings from the earlier whole-branch review (logger field initializer outside the never-throw guard; the raw↔frozen pairing test's missing non-emptiness precondition; the store reader's line-break escaping).
- Restoring the deleted README rules-table drift guard, or the deleted `BuildEnvironmentTest`.
- Cutting a release, CI workflow, sources/javadoc jars, `Automatic-Module-Name`.
