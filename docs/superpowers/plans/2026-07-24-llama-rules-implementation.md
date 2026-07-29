# LLamaRules (Central ArchUnit Rules Framework) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `io.github.milczekt1:llama-rules` — a reusable, test-scoped Maven library of centralized ArchUnit rules that any Java module adopts via one dependency plus one thin test class, where every rule is frozen (only *new* violations fail) and every failure prints why the rule exists, how to fix it, and how not to cheat it.

**Architecture:** Each rule is a `public static final ArchRule` field on a *group* class, produced by `FrozenRules.freeze(rawRule, ruleDoc)`. That helper pins the rule description to the `RuleDoc`'s short stable `id` via `.as(id)`, enables `.allowEmptyShould(true)`, registers the doc in `RuleRegistry`, and wraps the result in `FreezingArchRule`. Because ArchUnit derives the freeze-store key **from the rule description**, one short id serves as both the store key and the lookup key that `AgentFriendlyFailureDisplayFormat` (an ArchUnit `FailureDisplayFormat` SPI implementation) uses to re-attach the rich prose at failure time. Consumers wire groups in with `@AnalyzeClasses` + `ArchTests.in(...)`.

**Tech Stack:** Java 25, Maven (multi-module), ArchUnit 1.4.2 (`archunit-junit5`), JUnit Jupiter 5.11.3.

---

## Verified facts (spikes already run — do not re-litigate these)

These were confirmed empirically against ArchUnit 1.4.2 on JDK 25 before this plan was written. Trust them; they shape the code below.

1. **ArchUnit 1.4.2 reads Java 25 bytecode.** A class using sealed interfaces, records, and pattern-matching `switch` compiled with `--release 25` imported cleanly.
2. **Maven 3.8.6 defaults to `maven-compiler-plugin` 3.1, which silently ignores `maven.compiler.release`** and fails with *"Source option 5 is no longer supported"*. The plugin **must** be pinned (3.13.0+). The repo's current `pom.xml` has this latent bug.
3. **`FailureDisplayFormat` SPI** is exactly:
   `String formatFailure(HasDescription rule, FailureMessages failureMessages, Priority priority)`.
   `FailureMessages extends ForwardingList<String>` — it *is* a `List<String>` — and adds `getInformationAboutNumberOfViolations()`.
4. **The formatter is instantiated reflectively** by `FailureDisplayFormatFactory` from the `failureDisplayFormat` property. It must be a **public class with a public no-arg constructor**.
5. **ArchUnit's default failure text** (needed verbatim for the fallback path) is:
   `String.format("Architecture Violation [Priority: %s] - Rule '%s' was violated (%s):%n%s", priority.asString(), rule.getDescription(), failureMessages.getInformationAboutNumberOfViolations(), violationTexts)` where `violationTexts` joins the messages with `System.lineSeparator()`.
6. **The freeze-store key is literally `rule.getDescription()`** (`TextFileBasedViolationStore`), and `FreezingArchRule.getDescription()` delegates to the wrapped rule. A spike produced `stored.rules` containing `spike.no-stdout=<uuid>` — confirming `.as(id)` yields a short stable key.
7. **The formatter fires for frozen rules** with `rule.getDescription()` equal to the id. Chain: `FreezingArchRule.check` → `ArchRule$Assertions.check` → `assertNoViolation` → `FailureReport.toString()` → `formatFailure(...)`.
8. **The freeze loop works:** first run seeded the pre-existing violation and passed; after adding a *new* violation, the run failed reporting **only** the new one (`count=1`).
9. **The JUnit test tree exposes a per-rule leaf** named `<GroupClass> > <fieldName>` (observed: `SpikeRules > noStdout`), so a single rule is individually runnable.
10. **All five rule predicates in this plan compile** against ArchUnit 1.4.2 — including `noMethods()`, `dependOnClassesThat()...orShould().dependOnClassesThat()`, and `containAnyMethodsThat(...)`.
11. **`noClasses().should(customCondition)` inverts:** a `SimpleConditionEvent.satisfied(...)` event becomes a *violation*. Confirmed by spike.
12. **`org.springframework.test.context.bean.override.mockito.MockitoBean` exists in `spring-test` 7.0.8.** `org.springframework.boot.test.mock.mockito.MockBean` is removed in Spring Boot 4 — it is kept as an FQN string for older consumers and is verified as a configured constant, not via a fixture.
13. **Only `@ArchTest`-annotated fields** are collected by `ArchTests.in(...)`. Non-annotated static rule fields on a group class are ignored, which is what lets each group hold both a raw rule (for unit tests) and its frozen counterpart.

---

## Global Constraints

- Java release **25** (`maven.compiler.release=25`); `maven-compiler-plugin` pinned to **3.13.0**.
- ArchUnit **1.4.2**; JUnit Jupiter **5.11.3**; `maven-surefire-plugin` **3.5.2**.
- Root groupId `io.github.milczekt1`; version `0.1.0-SNAPSHOT`; MIT license; `project.build.sourceEncoding=UTF-8`.
- All framework production code lives under package **`io.github.milczekt1.archrules`** (per user decision; the spec's `com.yourorg.archrules` was a placeholder).
- The published library artifact is **`io.github.milczekt1:llama-rules`** (jar). The root pom is an aggregator named **`llama-rules-parent`** (packaging `pom`).
- `archunit-junit5` is a **compile-scope** dependency of `llama-rules` so consumers inherit the JUnit 5 engine transitively from one test-scoped dependency.
- The framework JAR **ships no frozen violations**. Freeze stores are per-consumer and committed by the consumer.
- Every rule is created through `FrozenRules.freeze(...)`, which unconditionally applies `.as(doc.id())` and `.allowEmptyShould(true)`.
- Rule ids match `^[a-z0-9]+(\.[a-z0-9-]+)+$` and are globally unique. **Changing an id is a breaking change** — it orphans every consumer's frozen entry.
- Rule prose is **generic** (per user decision): no org-, vendor-, or stack-specific references such as CockroachDB or a bespoke `Transactor` type.
- Types and annotations are matched **by fully-qualified name string**, never by class literal, so a consumer missing an optional dependency never breaks a rule.
- `AgentFriendlyFailureDisplayFormat` **must never throw** and must fall back to ArchUnit's default rendering for any rule whose description is not a registered `RuleDoc` id — `failureDisplayFormat` is a global per-run setting that also sees the consumer's own unrelated rules.
- Consumers must **not** use `ImportOption.DoNotIncludeTests` — `TestingRules` inspects test classes; excluding them makes those rules pass vacuously.
- Distribution: GitHub Packages (`https://maven.pkg.github.com/MilczekT1/LLamaRules`). The example module is **not** deployed.

---

## File Structure

```text
pom.xml                                   # MODIFY: aggregator llama-rules-parent (packaging=pom)
llama-rules/
  pom.xml                                 # CREATE: the published jar
  src/main/java/io/github/milczekt1/archrules/
    RuleDoc.java                          # value type: id / why / howToFix / howNotToFix + builder
    RuleRegistry.java                     # id -> RuleDoc map; register / find / all
    FrozenRules.java                      # freeze(ArchRule, RuleDoc) -> registered, named, frozen rule
    format/
      AntiFixPolicy.java                  # baseline global clauses, append-only
      AgentFriendlyFailureDisplayFormat.java  # FailureDisplayFormat SPI impl + default fallback
    groups/
      DatabaseRules.java                  # 3 rules
      TestingRules.java                   # 2 rules
      AllCentralRules.java                # aggregates groups; loadAll() for tests/docs
  src/test/java/io/github/milczekt1/archrules/
    RuleDocTest.java
    RuleRegistryTest.java
    format/AntiFixPolicyTest.java
    format/AgentFriendlyFailureDisplayFormatTest.java
    FrozenRulesTest.java
    groups/DatabaseRulesTest.java
    groups/TestingRulesTest.java
    groups/RuleRegistryCompletenessTest.java
    groups/FreezingBehaviourTest.java     # end-to-end seed/pass, new-violation/fail
    ReadmeRulesTableTest.java             # docs-drift guard
    fixtures/database/...                 # compliant + violating samples
    fixtures/testing/...                  # compliant + violating samples
examples/consumer-junit5/
  pom.xml                                 # CREATE: not deployed
  src/main/java/com/example/consumer/...  # one compliant class, one deliberate violation
  src/test/java/com/example/consumer/CentralArchitectureTest.java
  src/test/resources/archunit.properties
  src/test/resources/archunit/frozen/     # committed store
README.md                                 # MODIFY: generated-and-verified rules table
```

**Decomposition rationale:** `RuleDoc`, `RuleRegistry`, `AntiFixPolicy`, and the formatter are each one small responsibility with its own test cycle, and the formatter depends on all three — so they are built bottom-up. `FrozenRules` is the linchpin and gets its own task because it is where the id/description/store-key invariant is established. Each rule group is one task because a reviewer could reasonably accept `DatabaseRules` while rejecting `TestingRules`. Fixtures live beside the group test that needs them.

**Test fixture strategy:** framework tests use **real** test-scoped `spring-tx`, `spring-test`, and `mockito-core` so the forbidden-annotation FQNs are verified against the actual artifacts rather than hand-copied strings. The example consumer, by contrast, uses **zero** extra dependencies — its deliberate violation is raw `java.sql` usage from the JDK.

---

## Task 1: Multi-module skeleton and build configuration

**Files:**
- Modify: `pom.xml` (whole file — becomes the aggregator)
- Create: `llama-rules/pom.xml`
- Delete: empty dirs `src/main/java/io/github/milczekt1/llamarules`, `src/test/java/io/github/milczekt1/llamarules`, and the now-empty root `src/`
- Create: `llama-rules/src/test/java/io/github/milczekt1/archrules/BuildEnvironmentTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: the `llama-rules` module with `archunit-junit5` 1.4.2 on the compile classpath and `spring-tx` / `spring-test` / `mockito-core` on the test classpath; property `archunit.version`; every later task's code compiles against this.

- [ ] **Step 1: Write the failing test**

This task's deliverable is build configuration, so the test asserts the two things that are silently wrong today: that the compiler release actually took effect (constraint #2 above), and that ArchUnit is really on the classpath.

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/BuildEnvironmentTest.java`:

```java
package io.github.milczekt1.archrules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class BuildEnvironmentTest {

    /**
     * Guards against maven-compiler-plugin silently ignoring {@code maven.compiler.release}.
     * Class file major version 69 == Java 25.
     */
    @Test
    void compilesToJava25Bytecode() throws IOException {
        String resource = "/" + BuildEnvironmentTest.class.getName().replace('.', '/') + ".class";
        try (InputStream in = BuildEnvironmentTest.class.getResourceAsStream(resource);
             DataInputStream data = new DataInputStream(in)) {
            assertEquals(0xCAFEBABE, data.readInt(), "not a class file");
            data.readUnsignedShort(); // minor version
            assertEquals(69, data.readUnsignedShort(), "expected Java 25 (major 69) bytecode");
        }
    }

    @Test
    void archUnitCanImportJava25Bytecode() {
        var classes = new ClassFileImporter().importPackages("io.github.milczekt1.archrules");
        assertTrue(classes.size() > 0, "ArchUnit imported no classes");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -q test`

Expected: FAIL — the reactor does not yet contain a `llama-rules` module, so the build errors before the test runs (`Child module .../llama-rules does not exist` or a compilation failure on the missing ArchUnit import).

- [ ] **Step 3: Rewrite the root pom as an aggregator**

Replace the entire contents of `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>io.github.milczekt1</groupId>
  <artifactId>llama-rules-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <name>LLamaRules Parent</name>
  <description>Library of rules that guards the code.</description>
  <url>https://github.com/MilczekT1/LLamaRules</url>

  <licenses>
    <license>
      <name>MIT License</name>
      <url>https://opensource.org/licenses/MIT</url>
    </license>
  </licenses>

  <modules>
    <module>llama-rules</module>
    <module>examples/consumer-junit5</module>
  </modules>

  <properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <archunit.version>1.4.2</archunit.version>
    <junit.version>5.11.3</junit.version>
    <spring.version>7.0.8</spring.version>
    <mockito.version>5.23.0</mockito.version>
  </properties>

  <distributionManagement>
    <repository>
      <id>github</id>
      <name>GitHub Packages</name>
      <url>https://maven.pkg.github.com/MilczekT1/LLamaRules</url>
    </repository>
  </distributionManagement>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.tngtech.archunit</groupId>
        <artifactId>archunit-junit5</artifactId>
        <version>${archunit.version}</version>
      </dependency>
      <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>${junit.version}</version>
      </dependency>
      <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-tx</artifactId>
        <version>${spring.version}</version>
      </dependency>
      <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-test</artifactId>
        <version>${spring.version}</version>
      </dependency>
      <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>${mockito.version}</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <!-- Pinned deliberately: Maven 3.8.x defaults to compiler plugin 3.1,
             which ignores maven.compiler.release and fails with "Source option 5". -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.13.0</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>3.5.2</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-deploy-plugin</artifactId>
          <version>3.1.3</version>
        </plugin>
      </plugins>
    </pluginManagement>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

> The `examples/consumer-junit5` module is listed now but created in Task 11. Until then the reactor will not resolve. **Comment that `<module>` line out** while working through Tasks 1–10 and uncomment it in Task 11.

- [ ] **Step 4: Create the library module pom**

Create `llama-rules/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>io.github.milczekt1</groupId>
    <artifactId>llama-rules-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>

  <artifactId>llama-rules</artifactId>
  <packaging>jar</packaging>

  <name>LLamaRules</name>
  <description>Centralized, freezable ArchUnit rules with agent-friendly failure messages.</description>

  <dependencies>
    <!-- compile scope on purpose: consumers add ONE test-scoped dependency on llama-rules
         and inherit the archunit-junit5 engine transitively. -->
    <dependency>
      <groupId>com.tngtech.archunit</groupId>
      <artifactId>archunit-junit5</artifactId>
    </dependency>

    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <!-- test-only: provide the REAL forbidden annotations so the rules are verified
         against actual artifacts rather than hand-copied FQN strings. -->
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-tx</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-core</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

- [ ] **Step 5: Remove the stale root source tree**

The existing `src/` holds only empty `io/github/milczekt1/llamarules` package directories (the package changed to `archrules` and the sources moved into the module).

```bash
rm -rf src target
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `mvn -B -q test`

Expected: PASS — `BuildEnvironmentTest` reports 2 tests, 0 failures. If `compilesToJava25Bytecode` fails with major version 52, the compiler plugin pin did not take effect.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "build: restructure into llama-rules-parent aggregator with pinned toolchain"
```

---

## Task 2: `RuleDoc` — the structured per-rule documentation value type

**Files:**
- Create: `llama-rules/src/main/java/io/github/milczekt1/archrules/RuleDoc.java`
- Test: `llama-rules/src/test/java/io/github/milczekt1/archrules/RuleDocTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `RuleDoc` record with accessors `String id()`, `String why()`, `String howToFix()`, `Optional<String> howNotToFix()`; static `RuleDoc.Builder builder()`; builder methods `id(String)`, `why(String)`, `howToFix(String)`, `howNotToFix(String)`, `build()`. Construction throws `IllegalArgumentException` on blank required fields or a malformed id. Used by Tasks 3, 5, 6, 7, 8, 9, 12.

- [ ] **Step 1: Write the failing test**

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/RuleDocTest.java`:

```java
package io.github.milczekt1.archrules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RuleDocTest {

    private static RuleDoc.Builder valid() {
        return RuleDoc.builder()
                .id("db.no-spring-transactional")
                .why("because reasons")
                .howToFix("do this instead");
    }

    @Test
    void buildsWithAllFields() {
        RuleDoc doc = valid().howNotToFix("do not do that").build();

        assertEquals("db.no-spring-transactional", doc.id());
        assertEquals("because reasons", doc.why());
        assertEquals("do this instead", doc.howToFix());
        assertEquals(Optional.of("do not do that"), doc.howNotToFix());
    }

    @Test
    void howNotToFixIsOptionalAndDefaultsToEmpty() {
        assertEquals(Optional.empty(), valid().build().howNotToFix());
    }

    @Test
    void rejectsBlankRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> valid().why("   ").build());
        assertThrows(IllegalArgumentException.class, () -> valid().howToFix(null).build());
        assertThrows(IllegalArgumentException.class, () -> valid().id("").build());
    }

    @Test
    void rejectsIdsThatWouldMakeUnstableOrUnreadableFreezeKeys() {
        // The id IS the freeze-store key: no spaces, no upper case, must be dot-namespaced.
        assertThrows(IllegalArgumentException.class, () -> valid().id("noSpringTransactional").build());
        assertThrows(IllegalArgumentException.class, () -> valid().id("db.No Spring Tx").build());
        assertThrows(IllegalArgumentException.class, () -> valid().id("DB.no-spring-tx").build());
    }

    @Test
    void blankFieldMessageNamesTheOffendingField() {
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> valid().why(" ").build());
        assertTrue(e.getMessage().contains("why"), "message should name the field: " + e.getMessage());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -q -pl llama-rules test -Dtest=RuleDocTest`

Expected: FAIL — compilation error, `cannot find symbol: class RuleDoc`.

- [ ] **Step 3: Write the implementation**

Create `llama-rules/src/main/java/io/github/milczekt1/archrules/RuleDoc.java`:

```java
package io.github.milczekt1.archrules;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Structured, agent-facing documentation for a single architecture rule.
 *
 * <p>The {@link #id()} is deliberately short and stable: it becomes the ArchUnit rule
 * description, which in turn is the key ArchUnit uses for the freeze store <em>and</em> the key
 * {@code AgentFriendlyFailureDisplayFormat} uses to look this doc back up. Rich prose is kept
 * out of the description on purpose — rewording it would otherwise silently re-seed every
 * consumer's freeze store.
 *
 * <p><strong>Changing an id is a breaking change.</strong>
 */
public record RuleDoc(String id, String why, String howToFix, Optional<String> howNotToFix) {

    /** Lower-case, dot-namespaced, kebab-cased segments — e.g. {@code db.no-spring-transactional}. */
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9]+(\\.[a-z0-9-]+)+$");

    public RuleDoc {
        requireText(id, "id");
        requireText(why, "why");
        requireText(howToFix, "howToFix");
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "id '" + id + "' must match " + ID_PATTERN.pattern()
                            + " — it is the freeze-store key and must stay short and stable");
        }
        if (howNotToFix == null) {
            throw new IllegalArgumentException("howNotToFix must not be null (use Optional.empty())");
        }
        howNotToFix = howNotToFix.map(String::trim).filter(s -> !s.isEmpty());
    }

    public static Builder builder() {
        return new Builder();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be null or blank");
        }
    }

    public static final class Builder {
        private String id;
        private String why;
        private String howToFix;
        private String howNotToFix;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder why(String why) {
            this.why = why;
            return this;
        }

        public Builder howToFix(String howToFix) {
            this.howToFix = howToFix;
            return this;
        }

        public Builder howNotToFix(String howNotToFix) {
            this.howNotToFix = howNotToFix;
            return this;
        }

        public RuleDoc build() {
            return new RuleDoc(id, why, howToFix, Optional.ofNullable(howNotToFix));
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B -q -pl llama-rules test -Dtest=RuleDocTest`

Expected: PASS — 5 tests, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add llama-rules/src/main/java/io/github/milczekt1/archrules/RuleDoc.java \
        llama-rules/src/test/java/io/github/milczekt1/archrules/RuleDocTest.java
git commit -m "feat: add RuleDoc value type with stable-id validation"
```

---

## Task 3: `RuleRegistry` — id-to-doc lookup

**Files:**
- Create: `llama-rules/src/main/java/io/github/milczekt1/archrules/RuleRegistry.java`
- Test: `llama-rules/src/test/java/io/github/milczekt1/archrules/RuleRegistryTest.java`

**Interfaces:**
- Consumes: `RuleDoc` (Task 2).
- Produces: `RuleRegistry.register(RuleDoc)`, `Optional<RuleDoc> RuleRegistry.find(String id)`, `List<RuleDoc> RuleRegistry.all()` (sorted by id). Re-registering an identical doc is a no-op; re-registering a *different* doc under the same id throws `IllegalStateException`. Used by Tasks 5, 6, 9, 12.

> **Design note — no group auto-loading here.** `RuleRegistry` deliberately does not reach into
> `groups.*`; that would be a cycle (groups → registry → groups) and risks partially-initialized
> classes. Registration happens as a side effect of each group class's static initialiser calling
> `FrozenRules.freeze(...)`. Callers that need *every* doc (the completeness test, the README test)
> force group loading explicitly via `AllCentralRules.loadAll()` from Task 9. At failure time the
> group class is already loaded — the rule came from it — so the formatter always finds its doc.

- [ ] **Step 1: Write the failing test**

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/RuleRegistryTest.java`:

```java
package io.github.milczekt1.archrules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RuleRegistryTest {

    private static RuleDoc doc(String id, String why) {
        return RuleDoc.builder().id(id).why(why).howToFix("fix it").build();
    }

    @Test
    void registersAndFindsById() {
        RuleDoc registered = doc("registry.find-me", "because");
        RuleRegistry.register(registered);

        assertEquals(Optional.of(registered), RuleRegistry.find("registry.find-me"));
    }

    @Test
    void findReturnsEmptyForUnknownId() {
        assertEquals(Optional.empty(), RuleRegistry.find("registry.never-registered"));
    }

    @Test
    void findToleratesNullAndArbitraryDescriptions() {
        // The formatter passes ArchUnit rule descriptions straight through, including
        // full sentences from a consumer's own rules. This must never blow up.
        assertEquals(Optional.empty(), RuleRegistry.find(null));
        assertEquals(Optional.empty(), RuleRegistry.find("no classes should be annotated with @Foo"));
    }

    @Test
    void reRegisteringTheSameDocIsIdempotent() {
        RuleRegistry.register(doc("registry.idempotent", "because"));
        RuleRegistry.register(doc("registry.idempotent", "because"));

        assertEquals(Optional.of(doc("registry.idempotent", "because")),
                RuleRegistry.find("registry.idempotent"));
    }

    @Test
    void rejectsTwoDifferentDocsSharingAnId() {
        RuleRegistry.register(doc("registry.clash", "first reason"));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> RuleRegistry.register(doc("registry.clash", "conflicting reason")));
        assertTrue(e.getMessage().contains("registry.clash"), e.getMessage());
    }

    @Test
    void allIsSortedByIdAndContainsRegisteredDocs() {
        RuleRegistry.register(doc("registry.zzz-last", "because"));
        RuleRegistry.register(doc("registry.aaa-first", "because"));

        List<String> ids = RuleRegistry.all().stream().map(RuleDoc::id).toList();

        assertEquals(ids.stream().sorted().toList(), ids, "all() must be sorted by id");
        assertTrue(ids.contains("registry.aaa-first"));
        assertTrue(ids.contains("registry.zzz-last"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -q -pl llama-rules test -Dtest=RuleRegistryTest`

Expected: FAIL — compilation error, `cannot find symbol: class RuleRegistry`.

- [ ] **Step 3: Write the implementation**

Create `llama-rules/src/main/java/io/github/milczekt1/archrules/RuleRegistry.java`:

```java
package io.github.milczekt1.archrules;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global lookup from a rule's stable id to its {@link RuleDoc}.
 *
 * <p>Populated as a side effect of {@link FrozenRules#freeze}, i.e. when a group class is
 * initialised. The failure formatter reads from here to re-attach rich prose to a violation.
 */
public final class RuleRegistry {

    private static final Map<String, RuleDoc> DOCS = new ConcurrentHashMap<>();

    private RuleRegistry() {
    }

    /**
     * @throws IllegalStateException if a <em>different</em> doc is already registered under this id
     */
    public static void register(RuleDoc doc) {
        RuleDoc existing = DOCS.putIfAbsent(doc.id(), doc);
        if (existing != null && !existing.equals(doc)) {
            throw new IllegalStateException(
                    "Duplicate rule id '" + doc.id() + "': it is already registered with different"
                            + " documentation. Rule ids are freeze-store keys and must be globally unique.");
        }
    }

    /** Never throws; an unknown or null description simply yields {@link Optional#empty()}. */
    public static Optional<RuleDoc> find(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(DOCS.get(id));
    }

    /** Every doc registered <em>so far</em>, sorted by id. See the design note on group loading. */
    public static List<RuleDoc> all() {
        return DOCS.values().stream().sorted(Comparator.comparing(RuleDoc::id)).toList();
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B -q -pl llama-rules test -Dtest=RuleRegistryTest`

Expected: PASS — 6 tests, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add llama-rules/src/main/java/io/github/milczekt1/archrules/RuleRegistry.java \
        llama-rules/src/test/java/io/github/milczekt1/archrules/RuleRegistryTest.java
git commit -m "feat: add RuleRegistry keyed by stable rule id"
```

---

## Task 4: `AntiFixPolicy` — the extendable global anti-cheat clauses

**Files:**
- Create: `llama-rules/src/main/java/io/github/milczekt1/archrules/format/AntiFixPolicy.java`
- Test: `llama-rules/src/test/java/io/github/milczekt1/archrules/format/AntiFixPolicyTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `List<String> AntiFixPolicy.clauses()` (baseline clauses first, then appended ones, in insertion order) and `void AntiFixPolicy.addClause(String)`. There is deliberately **no** remove/replace API — the baseline can be extended but never silently dropped. Used by Task 5.

- [ ] **Step 1: Write the failing test**

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/format/AntiFixPolicyTest.java`:

```java
package io.github.milczekt1.archrules.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AntiFixPolicyTest {

    @Test
    void baselineCoversEveryDocumentedCheat() {
        String all = String.join("\n", AntiFixPolicy.clauses()).toLowerCase();

        assertTrue(all.contains("archunit/frozen"), "must forbid editing the freeze store");
        assertTrue(all.contains("@suppresswarnings"), "must forbid suppressing");
        assertTrue(all.contains("@archignore"), "must forbid @ArchIgnore");
        assertTrue(all.contains("rename"), "must forbid renaming to dodge name-based rules");
        assertTrue(all.contains("@analyzeclasses"), "must forbid narrowing the scan");
        assertTrue(all.contains("importoption"), "must forbid hiding code with ImportOptions");
        assertTrue(all.contains("weaken"), "must forbid weakening the rule");
    }

    @Test
    void baselineEndsWithTheOnlyAcceptableResolution() {
        List<String> clauses = AntiFixPolicy.clauses();
        String last = clauses.get(clauses.size() - 1).toLowerCase();

        assertTrue(last.contains("only"), "final clause must state the only acceptable resolution");
        assertTrue(last.contains("genuinely passes"), "final clause: " + last);
    }

    @Test
    void addedClausesAppendAfterTheBaselineAndNeverReplaceIt() {
        List<String> baseline = AntiFixPolicy.clauses();

        AntiFixPolicy.addClause("Do NOT disable the module in CI.");
        List<String> extended = AntiFixPolicy.clauses();

        assertEquals(baseline.size() + 1, extended.size());
        assertEquals(baseline, extended.subList(0, baseline.size()), "baseline must be preserved verbatim");
        assertEquals("Do NOT disable the module in CI.", extended.get(extended.size() - 1));
    }

    @Test
    void clausesIsUnmodifiableSoCallersCannotStripTheBaseline() {
        assertThrows(UnsupportedOperationException.class, () -> AntiFixPolicy.clauses().clear());
    }

    @Test
    void rejectsBlankClauses() {
        assertThrows(IllegalArgumentException.class, () -> AntiFixPolicy.addClause("  "));
        assertThrows(IllegalArgumentException.class, () -> AntiFixPolicy.addClause(null));
    }
}
```

> **Ordering note:** `addedClausesAppendAfterTheBaselineAndNeverReplaceIt` mutates global state.
> It reads the baseline size at the start rather than hard-coding a count, so it stays correct
> regardless of test execution order.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -q -pl llama-rules test -Dtest=AntiFixPolicyTest`

Expected: FAIL — compilation error, `package io.github.milczekt1.archrules.format does not exist`.

- [ ] **Step 3: Write the implementation**

Create `llama-rules/src/main/java/io/github/milczekt1/archrules/format/AntiFixPolicy.java`:

```java
package io.github.milczekt1.archrules.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The global "how NOT to fix this" policy rendered at the bottom of <em>every</em> framework
 * rule failure.
 *
 * <p>Rule authors and consumers may {@link #addClause append} project-specific clauses. There is
 * intentionally no API to remove or replace the baseline: the whole point is that the anti-cheat
 * guidance cannot be forgotten, weakened, or quietly dropped per rule.
 */
public final class AntiFixPolicy {

    private static final List<String> BASELINE = List.of(
            "Do NOT edit, hand-write, or delete files under archunit/frozen/ to make a NEW violation"
                    + " disappear. The store records pre-existing debt only; new violations must be fixed in code.",
            "Do NOT silence the rule with @SuppressWarnings, @ArchIgnore, comments, or by disabling the test.",
            "Do NOT rename a class, field, or package solely to dodge a name-based rule"
                    + " (e.g. renaming FooIT so the integration-test rule stops matching).",
            "Do NOT narrow @AnalyzeClasses(packages=...) or add ImportOptions to hide code from the scan.",
            "Do NOT downgrade, remove, reword, or otherwise weaken the rule.",
            "The ONLY acceptable resolution is changing the production/test code so the rule genuinely"
                    + " passes — then follow this rule's HOW TO FIX.");

    private static final List<String> ADDITIONAL = new CopyOnWriteArrayList<>();

    private AntiFixPolicy() {
    }

    /** Baseline clauses first, then any appended clauses, in insertion order. */
    public static List<String> clauses() {
        List<String> all = new ArrayList<>(BASELINE);
        all.addAll(ADDITIONAL);
        return Collections.unmodifiableList(all);
    }

    /** Appends a project-specific clause. The baseline is never affected. */
    public static void addClause(String clause) {
        if (clause == null || clause.isBlank()) {
            throw new IllegalArgumentException("clause must not be null or blank");
        }
        ADDITIONAL.add(clause.trim());
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B -q -pl llama-rules test -Dtest=AntiFixPolicyTest`

Expected: PASS — 5 tests, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add llama-rules/src/main/java/io/github/milczekt1/archrules/format/AntiFixPolicy.java \
        llama-rules/src/test/java/io/github/milczekt1/archrules/format/AntiFixPolicyTest.java
git commit -m "feat: add extendable global anti-fix policy"
```

---

## Task 5: `AgentFriendlyFailureDisplayFormat` — the failure renderer

**Files:**
- Create: `llama-rules/src/main/java/io/github/milczekt1/archrules/format/AgentFriendlyFailureDisplayFormat.java`
- Test: `llama-rules/src/test/java/io/github/milczekt1/archrules/format/AgentFriendlyFailureDisplayFormatTest.java`

**Interfaces:**
- Consumes: `RuleDoc` (Task 2), `RuleRegistry.find` (Task 3), `AntiFixPolicy.clauses` (Task 4).
- Produces: `public class AgentFriendlyFailureDisplayFormat implements FailureDisplayFormat` with a public no-arg constructor, registered via `failureDisplayFormat=io.github.milczekt1.archrules.format.AgentFriendlyFailureDisplayFormat`. Section headers it emits — relied on by Tasks 10 and 11 — are exactly `WHY:`, `HOW TO FIX:`, `HOW NOT TO FIX (this rule):`, `HOW NOT TO FIX (always):`, `Offending locations:`. Package-private seams `render(RuleDoc, List<String>, Priority)` and `defaultFormat(String, List<String>, String, Priority)` exist for unit testing.

> **Two constraints drive this design.**
> 1. The class and its no-arg constructor must be `public` — ArchUnit instantiates it reflectively (verified fact #4).
> 2. `FailureMessages` has **no public constructor**, so it cannot be built in a unit test. Therefore `formatFailure` is a thin adapter over two package-private seams that take a plain `List<String>` (legitimate: `FailureMessages` *is* a `List<String>`, verified fact #3). The seams are unit-tested here; the `formatFailure` adapter itself is covered end-to-end in Task 10, where a real frozen rule really fails.

- [ ] **Step 1: Write the failing test**

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/format/AgentFriendlyFailureDisplayFormatTest.java`:

```java
package io.github.milczekt1.archrules.format;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.lang.Priority;
import io.github.milczekt1.archrules.RuleDoc;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentFriendlyFailureDisplayFormatTest {

    private static final AgentFriendlyFailureDisplayFormat FORMAT =
            new AgentFriendlyFailureDisplayFormat();

    private static final List<String> VIOLATIONS = List.of(
            "Class <com.example.OrderService> is annotated with @Transactional in (OrderService.java:12)",
            "Class <com.example.StockService> is annotated with @Transactional in (StockService.java:8)");

    private static final RuleDoc DOCUMENTED = RuleDoc.builder()
            .id("format.documented-rule")
            .why("This rule exists to protect invariant X.")
            .howToFix("Do Y instead.")
            .howNotToFix("Do NOT simply swap in Z — every variant is banned.")
            .build();

    private static final RuleDoc TERSE = RuleDoc.builder()
            .id("format.terse-rule")
            .why("Because invariant W matters.")
            .howToFix("Fix it by doing V.")
            .build();

    @Test
    void rendersEverySectionForADocumentedRule() {
        String out = FORMAT.render(DOCUMENTED, VIOLATIONS, Priority.MEDIUM);

        assertTrue(out.contains("Architecture Violation [format.documented-rule]"), out);
        assertTrue(out.contains("WHY:"), out);
        assertTrue(out.contains("This rule exists to protect invariant X."), out);
        assertTrue(out.contains("HOW TO FIX:"), out);
        assertTrue(out.contains("Do Y instead."), out);
        assertTrue(out.contains("HOW NOT TO FIX (this rule):"), out);
        assertTrue(out.contains("Do NOT simply swap in Z"), out);
        assertTrue(out.contains("HOW NOT TO FIX (always):"), out);
        assertTrue(out.contains("Offending locations:"), out);
    }

    @Test
    void sectionsAppearInTeachingOrder() {
        String out = FORMAT.render(DOCUMENTED, VIOLATIONS, Priority.MEDIUM);

        int why = out.indexOf("WHY:");
        int fix = out.indexOf("HOW TO FIX:");
        int perRule = out.indexOf("HOW NOT TO FIX (this rule):");
        int global = out.indexOf("HOW NOT TO FIX (always):");
        int locations = out.indexOf("Offending locations:");

        assertTrue(why < fix && fix < perRule && perRule < global && global < locations,
                "sections out of order:\n" + out);
    }

    @Test
    void alwaysRendersTheFullGlobalAntiFixPolicy() {
        String out = FORMAT.render(DOCUMENTED, VIOLATIONS, Priority.MEDIUM);

        for (String clause : AntiFixPolicy.clauses()) {
            assertTrue(out.contains(clause), "missing anti-fix clause: " + clause);
        }
    }

    @Test
    void omitsThePerRuleSectionWhenTheDocHasNoHowNotToFix() {
        String out = FORMAT.render(TERSE, VIOLATIONS, Priority.MEDIUM);

        assertFalse(out.contains("HOW NOT TO FIX (this rule):"), out);
        assertTrue(out.contains("HOW NOT TO FIX (always):"), "global policy is never optional: " + out);
    }

    @Test
    void includesEveryViolationLine() {
        String out = FORMAT.render(DOCUMENTED, VIOLATIONS, Priority.MEDIUM);

        for (String violation : VIOLATIONS) {
            assertTrue(out.contains(violation), "missing violation line: " + violation);
        }
    }

    @Test
    void defaultFormatMatchesArchUnitsOwnRenderingForForeignRules() {
        // failureDisplayFormat is global: a consumer's own rules pass through this formatter too,
        // and must come out looking exactly as they would without the framework installed.
        String description = "no classes should depend on classes that reside in a package '..internal..'";

        String out = FORMAT.defaultFormat(description, VIOLATIONS, "2 times", Priority.HIGH);

        String expected = String.format(
                "Architecture Violation [Priority: HIGH] - Rule '%s' was violated (2 times):%n%s",
                description, String.join(System.lineSeparator(), VIOLATIONS));
        org.junit.jupiter.api.Assertions.assertEquals(expected, out);
        assertFalse(out.contains("HOW NOT TO FIX (always):"), "must not decorate foreign rules: " + out);
    }

    @Test
    void neverThrowsWhenTheRuleDescriptionBlowsUp() {
        com.tngtech.archunit.base.HasDescription hostile = () -> {
            throw new IllegalStateException("boom");
        };

        // No FailureMessages available in a unit test, so exercise the guard via the adapter's
        // description lookup with a null message list standing in for a degenerate call.
        String out = FORMAT.describeSafely(hostile);

        assertTrue(out.contains("unknown rule"), "must degrade gracefully, got: " + out);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -q -pl llama-rules test -Dtest=AgentFriendlyFailureDisplayFormatTest`

Expected: FAIL — compilation error, `cannot find symbol: class AgentFriendlyFailureDisplayFormat`.

- [ ] **Step 3: Write the implementation**

Create `llama-rules/src/main/java/io/github/milczekt1/archrules/format/AgentFriendlyFailureDisplayFormat.java`:

```java
package io.github.milczekt1.archrules.format;

import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.lang.FailureDisplayFormat;
import com.tngtech.archunit.lang.FailureMessages;
import com.tngtech.archunit.lang.Priority;
import io.github.milczekt1.archrules.RuleDoc;
import io.github.milczekt1.archrules.RuleRegistry;
import java.util.List;
import java.util.Optional;

/**
 * Renders framework rule failures as agent- and human-readable guidance: WHY the rule exists,
 * HOW TO FIX it, the rule's own anti-fix trap, and the global {@link AntiFixPolicy}.
 *
 * <p>Register it in a consumer's {@code src/test/resources/archunit.properties}:
 * <pre>{@code failureDisplayFormat=io.github.milczekt1.archrules.format.AgentFriendlyFailureDisplayFormat}</pre>
 *
 * <p>{@code failureDisplayFormat} is a <strong>global per-run</strong> setting, so this formatter
 * also sees the consumer's own unrelated rules. Any description that is not a registered
 * {@link RuleDoc} id falls through to ArchUnit's standard rendering, and this class never throws:
 * a formatter must never mask a real architecture violation with its own stack trace.
 *
 * <p>Must stay {@code public} with a {@code public} no-arg constructor — ArchUnit instantiates it
 * reflectively from the configured class name.
 */
public class AgentFriendlyFailureDisplayFormat implements FailureDisplayFormat {

    private static final String INDENT = "  ";
    private static final String UNKNOWN_RULE = "<unknown rule>";

    @Override
    public String formatFailure(HasDescription rule, FailureMessages failureMessages, Priority priority) {
        String description = describeSafely(rule);
        String countInfo = countInfoSafely(failureMessages);
        List<String> lines = failureMessages == null ? List.of() : List.copyOf(failureMessages);
        try {
            Optional<RuleDoc> doc = RuleRegistry.find(description);
            return doc.isPresent()
                    ? render(doc.get(), lines, priority)
                    : defaultFormat(description, lines, countInfo, priority);
        } catch (RuntimeException e) {
            return defaultFormat(description, lines, countInfo, priority);
        }
    }

    /** Package-private seam: rendering a documented rule, testable with a plain list. */
    String render(RuleDoc doc, List<String> violationLines, Priority priority) {
        String nl = System.lineSeparator();
        StringBuilder out = new StringBuilder();

        out.append("Architecture Violation [").append(doc.id()).append(']')
                .append(" [Priority: ").append(priority.asString()).append(']').append(nl).append(nl);

        out.append("WHY:").append(nl).append(indent(doc.why())).append(nl).append(nl);
        out.append("HOW TO FIX:").append(nl).append(indent(doc.howToFix())).append(nl).append(nl);

        doc.howNotToFix().ifPresent(text ->
                out.append("HOW NOT TO FIX (this rule):").append(nl).append(indent(text)).append(nl).append(nl));

        out.append("HOW NOT TO FIX (always):").append(nl);
        for (String clause : AntiFixPolicy.clauses()) {
            out.append(INDENT).append("- ").append(clause).append(nl);
        }
        out.append(nl);

        out.append("Offending locations:").append(nl);
        for (String line : violationLines) {
            out.append(INDENT).append(line).append(nl);
        }
        return out.toString();
    }

    /** Package-private seam: byte-for-byte ArchUnit's default rendering, so foreign rules look untouched. */
    String defaultFormat(String description, List<String> violationLines, String countInfo, Priority priority) {
        String violationTexts = String.join(System.lineSeparator(), violationLines);
        return String.format("Architecture Violation [Priority: %s] - Rule '%s' was violated (%s):%n%s",
                priority.asString(), description, countInfo, violationTexts);
    }

    /** Package-private seam: a hostile or half-built rule must not break failure reporting. */
    String describeSafely(HasDescription rule) {
        try {
            String description = rule == null ? null : rule.getDescription();
            return description == null ? UNKNOWN_RULE : description;
        } catch (RuntimeException e) {
            return UNKNOWN_RULE;
        }
    }

    private static String countInfoSafely(FailureMessages messages) {
        try {
            return messages == null ? "0 times" : messages.getInformationAboutNumberOfViolations();
        } catch (RuntimeException e) {
            return "unknown number of times";
        }
    }

    private static String indent(String text) {
        return text.lines()
                .map(line -> INDENT + line)
                .reduce((a, b) -> a + System.lineSeparator() + b)
                .orElse("");
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B -q -pl llama-rules test -Dtest=AgentFriendlyFailureDisplayFormatTest`

Expected: PASS — 7 tests, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add llama-rules/src/main/java/io/github/milczekt1/archrules/format/AgentFriendlyFailureDisplayFormat.java \
        llama-rules/src/test/java/io/github/milczekt1/archrules/format/AgentFriendlyFailureDisplayFormatTest.java
git commit -m "feat: add agent-friendly failure display format with default fallback"
```

---

## Task 6: `FrozenRules.freeze` — the id / description / store-key linchpin

**Files:**
- Create: `llama-rules/src/main/java/io/github/milczekt1/archrules/FrozenRules.java`
- Test: `llama-rules/src/test/java/io/github/milczekt1/archrules/FrozenRulesTest.java`

**Interfaces:**
- Consumes: `RuleDoc` (Task 2), `RuleRegistry.register` (Task 3).
- Produces: `static ArchRule FrozenRules.freeze(ArchRule rule, RuleDoc doc)`. It registers `doc`, applies `.as(doc.id())` then `.allowEmptyShould(true)`, and wraps the result in `FreezingArchRule`. Every rule in Tasks 7 and 8 is built with it.

> **Why the order matters.** `.as(doc.id())` must be applied to the **delegate before** freezing:
> the freeze-store key is `rule.getDescription()` (verified fact #6), so pinning the description
> to the short id is what keeps the store key stable when the prose is reworded. This is the exact
> fragility the design set out to eliminate.

- [ ] **Step 1: Write the failing test**

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/FrozenRulesTest.java`:

```java
package io.github.milczekt1.archrules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FrozenRulesTest {

    @TempDir
    Path store;

    private static final RuleDoc DOC = RuleDoc.builder()
            .id("frozen.sample-rule")
            .why("Sample rule for the freeze plumbing.")
            .howToFix("Stop doing the thing.")
            .build();

    @BeforeEach
    void useTemporaryStore() {
        ArchConfiguration.get().setProperty("freeze.store.default.path", store.toString());
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreCreation", "true");
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreUpdate", "true");
    }

    @AfterEach
    void resetConfiguration() {
        ArchConfiguration.get().reset();
    }

    private static ArchRule rawRule() {
        return noClasses().that().haveSimpleNameEndingWith("Test")
                .should().haveSimpleNameStartingWith("Frozen");
    }

    @Test
    void pinsTheDescriptionToTheStableId() {
        ArchRule frozen = FrozenRules.freeze(rawRule(), DOC);

        assertEquals("frozen.sample-rule", frozen.getDescription(),
                "description IS the freeze-store key and must equal the RuleDoc id");
    }

    @Test
    void registersTheDocSoTheFormatterCanFindIt() {
        FrozenRules.freeze(rawRule(), DOC);

        assertEquals(Optional.of(DOC), RuleRegistry.find("frozen.sample-rule"));
    }

    @Test
    void writesTheFreezeStoreKeyedByTheId() throws Exception {
        ArchRule frozen = FrozenRules.freeze(rawRule(), DOC);

        frozen.check(new ClassFileImporter().importClasses(FrozenRulesTest.class));

        String storedRules = Files.readString(store.resolve("stored.rules"));
        assertTrue(storedRules.contains("frozen.sample-rule="),
                "store key must be the short id, was:\n" + storedRules);
    }

    @Test
    void firstRunSeedsExistingViolationsAndPasses() {
        ArchRule frozen = FrozenRules.freeze(rawRule(), DOC);

        // FrozenRulesTest violates rawRule(); freezing means adoption does not block.
        assertDoesNotThrow(() -> frozen.check(new ClassFileImporter().importClasses(FrozenRulesTest.class)));
    }

    @Test
    void allowsEmptyShouldSoModulesWithNoMatchingClassesStayGreen() {
        RuleDoc doc = RuleDoc.builder()
                .id("frozen.empty-should")
                .why("Nothing matches this in the test fixture.")
                .howToFix("N/A")
                .build();
        ArchRule frozen = FrozenRules.freeze(
                noClasses().that().haveSimpleNameEndingWith("NoSuchSuffixAnywhere")
                        .should().haveSimpleName("Whatever"),
                doc);

        // Without allowEmptyShould(true) this throws AssertionError about an empty should.
        assertDoesNotThrow(() -> frozen.check(new ClassFileImporter().importClasses(FrozenRulesTest.class)));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -q -pl llama-rules test -Dtest=FrozenRulesTest`

Expected: FAIL — compilation error, `cannot find symbol: class FrozenRules`.

- [ ] **Step 3: Write the implementation**

Create `llama-rules/src/main/java/io/github/milczekt1/archrules/FrozenRules.java`:

```java
package io.github.milczekt1.archrules;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

/**
 * Turns a raw {@link ArchRule} into the shape every central rule must have.
 *
 * <p>{@link #freeze} does three things, and the order matters:
 * <ol>
 *   <li>registers the {@link RuleDoc} so the failure formatter can look the prose back up;</li>
 *   <li>pins the rule description to the doc's short, stable {@code id} — ArchUnit derives the
 *       freeze-store key from the description, so this is what stops a reworded sentence from
 *       silently re-seeding every consumer's store;</li>
 *   <li>allows an empty {@code should}, so a module containing no matching classes stays green
 *       instead of failing vacuously.</li>
 * </ol>
 * Only then is the rule wrapped in a {@link FreezingArchRule}, so that adopting a new rule records
 * existing debt rather than blocking in-flight work.
 */
public final class FrozenRules {

    private FrozenRules() {
    }

    public static ArchRule freeze(ArchRule rule, RuleDoc doc) {
        RuleRegistry.register(doc);
        return FreezingArchRule.freeze(rule.as(doc.id()).allowEmptyShould(true));
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B -q -pl llama-rules test -Dtest=FrozenRulesTest`

Expected: PASS — 5 tests, 0 failures. If `writesTheFreezeStoreKeyedByTheId` shows a long English sentence as the key, `.as(doc.id())` was applied in the wrong place.

- [ ] **Step 5: Commit**

```bash
git add llama-rules/src/main/java/io/github/milczekt1/archrules/FrozenRules.java \
        llama-rules/src/test/java/io/github/milczekt1/archrules/FrozenRulesTest.java
git commit -m "feat: add FrozenRules.freeze pinning stable ids as freeze-store keys"
```

---

## Task 7: `DatabaseRules` group

**Files:**
- Create: `llama-rules/src/main/java/io/github/milczekt1/archrules/groups/DatabaseRules.java`
- Test: `llama-rules/src/test/java/io/github/milczekt1/archrules/groups/DatabaseRulesTest.java`
- Create (fixtures):
  - `llama-rules/src/test/java/io/github/milczekt1/archrules/fixtures/database/service/AnnotatedService.java`
  - `llama-rules/src/test/java/io/github/milczekt1/archrules/fixtures/database/service/AnnotatedMethodService.java`
  - `llama-rules/src/test/java/io/github/milczekt1/archrules/fixtures/database/service/RawJdbcService.java`
  - `llama-rules/src/test/java/io/github/milczekt1/archrules/fixtures/database/service/CleanService.java`
  - `llama-rules/src/test/java/io/github/milczekt1/archrules/fixtures/database/repository/OrderRepository.java`

**Interfaces:**
- Consumes: `RuleDoc` (Task 2), `FrozenRules.freeze` (Task 6).
- Produces: `public final class DatabaseRules` with three `@ArchTest public static final ArchRule` fields — `noSpringTransactionalOnClasses`, `noSpringTransactionalOnMethods`, `noRawJdbcOutsideRepositories` — plus package-private **raw** (unfrozen) counterparts `NO_TX_ON_CLASSES_RULE`, `NO_TX_ON_METHODS_RULE`, `NO_RAW_JDBC_RULE` for unit testing. Rule ids: `db.no-spring-transactional-on-classes`, `db.no-spring-transactional-on-methods`, `db.no-raw-jdbc-outside-repositories`. Referenced by Tasks 9, 11, 12.

> **Raw vs frozen.** Each rule exists twice: the raw `ArchRule` (what the predicate actually
> asserts) and the frozen, registered, id-pinned `@ArchTest` field consumers run. Tests exercise
> the **raw** rule so freezing does not mask the outcome. Only `@ArchTest`-annotated fields are
> collected by `ArchTests.in(...)` (verified fact #13), so the raw constants are invisible to
> consumers' test trees.

- [ ] **Step 1: Write the fixtures**

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/fixtures/database/service/AnnotatedService.java`:

```java
package io.github.milczekt1.archrules.fixtures.database.service;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AnnotatedService {
    public void doWork() {
    }
}
```

Create `.../fixtures/database/service/AnnotatedMethodService.java`:

```java
package io.github.milczekt1.archrules.fixtures.database.service;

import org.springframework.transaction.annotation.Transactional;

public class AnnotatedMethodService {

    @Transactional
    public void doWork() {
    }

    public void untouched() {
    }
}
```

Create `.../fixtures/database/service/RawJdbcService.java`:

```java
package io.github.milczekt1.archrules.fixtures.database.service;

import java.sql.Connection;
import java.sql.SQLException;

/** Raw JDBC outside a repository/dao/jdbc package — a violation. */
public class RawJdbcService {
    public void query(Connection connection) throws SQLException {
        connection.createStatement().execute("SELECT 1");
    }
}
```

Create `.../fixtures/database/service/CleanService.java`:

```java
package io.github.milczekt1.archrules.fixtures.database.service;

/** Violates none of the database rules. */
public class CleanService {
    public String describe() {
        return "clean";
    }
}
```

Create `.../fixtures/database/repository/OrderRepository.java`:

```java
package io.github.milczekt1.archrules.fixtures.database.repository;

import java.sql.Connection;
import java.sql.SQLException;

/** Raw JDBC INSIDE a repository package — allowed. */
public class OrderRepository {
    public void findAll(Connection connection) throws SQLException {
        connection.createStatement().execute("SELECT * FROM orders");
    }
}
```

- [ ] **Step 2: Write the failing test**

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/groups/DatabaseRulesTest.java`:

```java
package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseRulesTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.archrules.fixtures.database");

    private static List<String> violations(ArchRule rule) {
        return rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails();
    }

    private static String joined(ArchRule rule) {
        return String.join("\n", violations(rule));
    }

    @Test
    void flagsClassLevelTransactional() {
        String report = joined(DatabaseRules.NO_TX_ON_CLASSES_RULE);

        assertTrue(report.contains("AnnotatedService"), report);
        assertFalse(report.contains("CleanService"), report);
    }

    @Test
    void flagsMethodLevelTransactional() {
        String report = joined(DatabaseRules.NO_TX_ON_METHODS_RULE);

        assertTrue(report.contains("AnnotatedMethodService"), report);
        assertTrue(report.contains("doWork"), report);
        assertFalse(report.contains("untouched"), report);
    }

    @Test
    void classLevelRuleDoesNotDoubleReportMethodAnnotations() {
        String report = joined(DatabaseRules.NO_TX_ON_CLASSES_RULE);

        assertFalse(report.contains("AnnotatedMethodService"),
                "the class-level rule must only match class-level annotations: " + report);
    }

    @Test
    void flagsRawJdbcOutsideRepositoryPackages() {
        String report = joined(DatabaseRules.NO_RAW_JDBC_RULE);

        assertTrue(report.contains("RawJdbcService"), report);
    }

    @Test
    void allowsRawJdbcInsideRepositoryPackages() {
        String report = joined(DatabaseRules.NO_RAW_JDBC_RULE);

        assertFalse(report.contains("OrderRepository"),
                "repository packages are the sanctioned home for JDBC: " + report);
    }

    @Test
    void everyPublicRuleIsFrozenAndIdPinned() {
        assertEquals("db.no-spring-transactional-on-classes",
                DatabaseRules.noSpringTransactionalOnClasses.getDescription());
        assertEquals("db.no-spring-transactional-on-methods",
                DatabaseRules.noSpringTransactionalOnMethods.getDescription());
        assertEquals("db.no-raw-jdbc-outside-repositories",
                DatabaseRules.noRawJdbcOutsideRepositories.getDescription());
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn -B -q -pl llama-rules test -Dtest=DatabaseRulesTest`

Expected: FAIL — compilation error, `cannot find symbol: class DatabaseRules`.

- [ ] **Step 4: Write the implementation**

Create `llama-rules/src/main/java/io/github/milczekt1/archrules/groups/DatabaseRules.java`:

```java
package io.github.milczekt1.archrules.groups;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.archrules.FrozenRules;
import io.github.milczekt1.archrules.RuleDoc;

/**
 * Rules about how code talks to the database.
 *
 * <p>Each rule appears twice: a package-private raw {@code *_RULE} constant holding the bare
 * predicate (unit-tested directly), and the public {@code @ArchTest} field consumers run, which is
 * registered, id-pinned and frozen by {@link FrozenRules#freeze}. Only the annotated fields are
 * picked up by {@code ArchTests.in(DatabaseRules.class)}.
 */
public final class DatabaseRules {

    /** Matched by FQN string so the framework needs no Spring on its classpath. */
    private static final String SPRING_TRANSACTIONAL =
            "org.springframework.transaction.annotation.Transactional";

    private static final String JDBC_TEMPLATE = "org.springframework.jdbc.core.JdbcTemplate";

    /** Packages where hand-written SQL/JDBC is the sanctioned, reviewed home. */
    private static final String[] PERSISTENCE_PACKAGES = {
            "..repository..", "..repositories..", "..dao..", "..jdbc..", "..persistence.."
    };

    private DatabaseRules() {
    }

    // ---------------------------------------------------------------- no @Transactional on classes

    static final RuleDoc NO_TX_ON_CLASSES_DOC = RuleDoc.builder()
            .id("db.no-spring-transactional-on-classes")
            .why("Declarative @Transactional gives you no control over retries, and cannot express the "
                    + "retry-on-serialization-failure semantics that distributed SQL databases require. It also "
                    + "hides transaction boundaries behind a proxy, so self-invocation silently runs "
                    + "un-transacted.")
            .howToFix("Make the transaction boundary explicit with a programmatic wrapper — your project's "
                    + "Transactor, Spring's TransactionTemplate, or the equivalent — and wrap only the work "
                    + "that must be atomic.")
            .howNotToFix("Do NOT swap in a different flavour such as @Transactional(propagation = REQUIRES_NEW), "
                    + "readOnly = true, or a custom meta-annotation that is itself annotated with @Transactional. "
                    + "Every variant is banned; the problem is declarative transaction management, not one "
                    + "attribute of it.")
            .build();

    static final ArchRule NO_TX_ON_CLASSES_RULE = noClasses()
            .should().beAnnotatedWith(SPRING_TRANSACTIONAL);

    @ArchTest
    public static final ArchRule noSpringTransactionalOnClasses =
            FrozenRules.freeze(NO_TX_ON_CLASSES_RULE, NO_TX_ON_CLASSES_DOC);

    // ---------------------------------------------------------------- no @Transactional on methods

    static final RuleDoc NO_TX_ON_METHODS_DOC = RuleDoc.builder()
            .id("db.no-spring-transactional-on-methods")
            .why("Method-level @Transactional has the same problem as the class-level form — no retry "
                    + "control, proxy-bound boundaries — and is the more common way it sneaks back in.")
            .howToFix("Replace the annotation with an explicit programmatic transaction around the work the "
                    + "method performs.")
            .howNotToFix("Do NOT move the annotation up to the class, down to a helper, or onto an interface "
                    + "method to get it out of this rule's way. It is banned in every position.")
            .build();

    static final ArchRule NO_TX_ON_METHODS_RULE = noMethods()
            .should().beAnnotatedWith(SPRING_TRANSACTIONAL);

    @ArchTest
    public static final ArchRule noSpringTransactionalOnMethods =
            FrozenRules.freeze(NO_TX_ON_METHODS_RULE, NO_TX_ON_METHODS_DOC);

    // ---------------------------------------------------------------- no raw JDBC outside repositories

    static final RuleDoc NO_RAW_JDBC_DOC = RuleDoc.builder()
            .id("db.no-raw-jdbc-outside-repositories")
            .why("Raw JDBC scattered through services leaks persistence concerns into business logic, "
                    + "bypasses the connection and retry handling the persistence layer applies, and makes "
                    + "SQL impossible to review in one place.")
            .howToFix("Move the query behind a type in a repository, dao, jdbc, or persistence package and "
                    + "call that from the service.")
            .howNotToFix("Do NOT rename the offending class or move it into a package merely named "
                    + "'repository' while it keeps doing service work — the package boundary is meant to "
                    + "reflect a real layering decision, not to satisfy a matcher.")
            .build();

    static final ArchRule NO_RAW_JDBC_RULE = noClasses()
            .that().resideOutsideOfPackages(PERSISTENCE_PACKAGES)
            .should().dependOnClassesThat().resideInAnyPackage("java.sql..", "javax.sql..")
            .orShould().dependOnClassesThat().haveFullyQualifiedName(JDBC_TEMPLATE);

    @ArchTest
    public static final ArchRule noRawJdbcOutsideRepositories =
            FrozenRules.freeze(NO_RAW_JDBC_RULE, NO_RAW_JDBC_DOC);
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -B -q -pl llama-rules test -Dtest=DatabaseRulesTest`

Expected: PASS — 6 tests, 0 failures.

If `classLevelRuleDoesNotDoubleReportMethodAnnotations` fails, `beAnnotatedWith` is matching meta-annotations more broadly than expected — narrow the class-level rule rather than relaxing the test.

- [ ] **Step 6: Commit**

```bash
git add llama-rules/src/main/java/io/github/milczekt1/archrules/groups/DatabaseRules.java \
        llama-rules/src/test/java/io/github/milczekt1/archrules/groups/DatabaseRulesTest.java \
        llama-rules/src/test/java/io/github/milczekt1/archrules/fixtures/database
git commit -m "feat: add DatabaseRules group with transactional and raw-JDBC guards"
```

---

## Task 8: `TestingRules` group

**Files:**
- Create: `llama-rules/src/main/java/io/github/milczekt1/archrules/groups/TestingRules.java`
- Test: `llama-rules/src/test/java/io/github/milczekt1/archrules/groups/TestingRulesTest.java`
- Modify: `llama-rules/pom.xml` (surefire must not execute the fixture classes)
- Create (fixtures, all under `llama-rules/src/test/java/io/github/milczekt1/archrules/fixtures/testing/`):
  `OrderRepository.java`, `OrderDao.java`, `PaymentGateway.java`, `MockingRepositoryIT.java`,
  `MockingDaoIntegrationTest.java`, `MockingGatewayIT.java`, `PlainUnitTest.java`,
  `WellNamedTest.java`, `BadlyNamedTestCase.java`

**Interfaces:**
- Consumes: `RuleDoc` (Task 2), `FrozenRules.freeze` (Task 6).
- Produces: `public final class TestingRules` with two `@ArchTest public static final ArchRule` fields — `integrationTestsMustNotMockRepositoriesOrDaos`, `testClassNamingConvention` — plus package-private raw counterparts `NO_MOCKED_REPOS_IN_IT_RULE`, `TEST_NAMING_RULE`, and the package-private constant `List<String> FORBIDDEN_MOCK_ANNOTATIONS`. Rule ids: `test.no-mocked-repository-in-integration-test`, `test.class-naming-convention`. Referenced by Tasks 9, 11, 12.

> **Fixture classes must not run as tests.** Several fixtures are deliberately named `*Test`,
> `*TestCase`, and `*IT`, and some carry real `@Test` methods so the naming rule has something to
> match. Surefire's default includes (`**/Test*.java`, `**/*Test.java`, `**/*Tests.java`,
> `**/*TestCase.java`) would execute them. Step 1 excludes the fixtures package.

- [ ] **Step 1: Exclude fixtures from surefire**

In `llama-rules/pom.xml`, add a `<build>` section before the closing `</project>`:

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <!-- Fixtures are inputs to the architecture rules, not tests. Some are named
               *Test / *TestCase / *IT on purpose and would otherwise be executed. -->
          <excludes>
            <exclude>**/fixtures/**</exclude>
          </excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>
```

- [ ] **Step 2: Write the fixtures**

`fixtures/testing/OrderRepository.java`:

```java
package io.github.milczekt1.archrules.fixtures.testing;

public interface OrderRepository {
    String findById(String id);
}
```

`fixtures/testing/OrderDao.java`:

```java
package io.github.milczekt1.archrules.fixtures.testing;

public interface OrderDao {
    String load(String id);
}
```

`fixtures/testing/PaymentGateway.java`:

```java
package io.github.milczekt1.archrules.fixtures.testing;

/** Not a repository or dao — mocking this in an IT is fine. */
public interface PaymentGateway {
    boolean charge(long amountMinor);
}
```

`fixtures/testing/MockingRepositoryIT.java` — violation (`@Mock` + `*Repository`):

```java
package io.github.milczekt1.archrules.fixtures.testing;

import org.mockito.Mock;

public class MockingRepositoryIT {
    @Mock
    OrderRepository orderRepository;
}
```

`fixtures/testing/MockingDaoIntegrationTest.java` — violation (`@MockitoBean` + `*Dao`):

```java
package io.github.milczekt1.archrules.fixtures.testing;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class MockingDaoIntegrationTest {
    @MockitoBean
    OrderDao orderDao;
}
```

`fixtures/testing/MockingGatewayIT.java` — compliant (mocks a non-persistence collaborator):

```java
package io.github.milczekt1.archrules.fixtures.testing;

import org.mockito.Mock;

public class MockingGatewayIT {
    @Mock
    PaymentGateway paymentGateway;
}
```

`fixtures/testing/PlainUnitTest.java` — compliant (a *unit* test may mock a repository):

```java
package io.github.milczekt1.archrules.fixtures.testing;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class PlainUnitTest {
    @Mock
    OrderRepository orderRepository;

    @Test
    void doesSomething() {
    }
}
```

`fixtures/testing/WellNamedTest.java` — compliant naming:

```java
package io.github.milczekt1.archrules.fixtures.testing;

import org.junit.jupiter.api.Test;

public class WellNamedTest {
    @Test
    void doesSomething() {
    }
}
```

`fixtures/testing/BadlyNamedTestCase.java` — violates naming (has `@Test`, ends in neither `Test` nor `IT`):

```java
package io.github.milczekt1.archrules.fixtures.testing;

import org.junit.jupiter.api.Test;

public class BadlyNamedTestCase {
    @Test
    void surefireWillNeverRunMe() {
    }
}
```

- [ ] **Step 3: Write the failing test**

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/groups/TestingRulesTest.java`:

```java
package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class TestingRulesTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter()
            .importPackages("io.github.milczekt1.archrules.fixtures.testing");

    private static String report(ArchRule rule) {
        return String.join("\n", rule.allowEmptyShould(true).evaluate(FIXTURES).getFailureReport().getDetails());
    }

    @Test
    void flagsMockedRepositoryInAClassNamedIT() {
        String report = report(TestingRules.NO_MOCKED_REPOS_IN_IT_RULE);

        assertTrue(report.contains("MockingRepositoryIT"), report);
        assertTrue(report.contains("orderRepository"), report);
    }

    @Test
    void flagsMockedDaoInAClassNamedIntegrationTest() {
        String report = report(TestingRules.NO_MOCKED_REPOS_IN_IT_RULE);

        assertTrue(report.contains("MockingDaoIntegrationTest"), report);
        assertTrue(report.contains("orderDao"), report);
    }

    @Test
    void allowsMockingNonPersistenceCollaboratorsInIntegrationTests() {
        String report = report(TestingRules.NO_MOCKED_REPOS_IN_IT_RULE);

        assertFalse(report.contains("MockingGatewayIT"),
                "only Repository/Dao types are forbidden: " + report);
    }

    @Test
    void allowsUnitTestsToMockRepositories() {
        String report = report(TestingRules.NO_MOCKED_REPOS_IN_IT_RULE);

        assertFalse(report.contains("PlainUnitTest"),
                "the rule targets integration tests only: " + report);
    }

    @Test
    void flagsTestClassesThatSurefireWouldNeverRun() {
        String report = report(TestingRules.TEST_NAMING_RULE);

        assertTrue(report.contains("BadlyNamedTestCase"), report);
    }

    @Test
    void acceptsConventionallyNamedTestClasses() {
        String report = report(TestingRules.TEST_NAMING_RULE);

        assertFalse(report.contains("WellNamedTest"), report);
        assertFalse(report.contains("PlainUnitTest"), report);
    }

    @Test
    void coversEveryForbiddenMockAnnotationIncludingTheRemovedSpringBootOne() {
        // MockBean was removed in Spring Boot 4, so it has no fixture; it stays in the list for
        // consumers still on Boot 3 and is verified here as a configured constant.
        assertTrue(TestingRules.FORBIDDEN_MOCK_ANNOTATIONS.containsAll(java.util.List.of(
                "org.mockito.Mock",
                "org.springframework.test.context.bean.override.mockito.MockitoBean",
                "org.springframework.boot.test.mock.mockito.MockBean")));
    }

    @Test
    void everyPublicRuleIsFrozenAndIdPinned() {
        assertEquals("test.no-mocked-repository-in-integration-test",
                TestingRules.integrationTestsMustNotMockRepositoriesOrDaos.getDescription());
        assertEquals("test.class-naming-convention",
                TestingRules.testClassNamingConvention.getDescription());
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `mvn -B -q -pl llama-rules test -Dtest=TestingRulesTest`

Expected: FAIL — compilation error, `cannot find symbol: class TestingRules`.

- [ ] **Step 5: Write the implementation**

Create `llama-rules/src/main/java/io/github/milczekt1/archrules/groups/TestingRules.java`:

```java
package io.github.milczekt1.archrules.groups;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.github.milczekt1.archrules.FrozenRules;
import io.github.milczekt1.archrules.RuleDoc;
import java.util.List;

/**
 * Rules about test hygiene.
 *
 * <p>Like {@link DatabaseRules}, each rule exists as a package-private raw {@code *_RULE} constant
 * for unit testing plus a public frozen {@code @ArchTest} field for consumers.
 *
 * <p>These rules inspect <em>test</em> classes, which is why consumers must not configure
 * {@code ImportOption.DoNotIncludeTests} — doing so makes them pass vacuously.
 */
public final class TestingRules {

    /** Matched by FQN string, so a consumer missing any of these libraries still works. */
    static final List<String> FORBIDDEN_MOCK_ANNOTATIONS = List.of(
            "org.mockito.Mock",
            "org.springframework.test.context.bean.override.mockito.MockitoBean",
            // Removed in Spring Boot 4; retained for consumers still on Boot 3.
            "org.springframework.boot.test.mock.mockito.MockBean");

    private static final String JUNIT_TEST = "org.junit.jupiter.api.Test";

    private TestingRules() {
    }

    // ------------------------------------------- integration tests must not mock repositories/daos

    static final RuleDoc NO_MOCKED_REPOS_IN_IT_DOC = RuleDoc.builder()
            .id("test.no-mocked-repository-in-integration-test")
            .why("An integration test exists to prove the real wiring works — schema, queries, mapping "
                    + "and transactions included. Mocking the repository or dao removes exactly the layer the "
                    + "test was written to exercise, leaving a slow test that proves nothing.")
            .howToFix("Let the integration test hit the real persistence layer against a real database "
                    + "(Testcontainers or an equivalent). If you genuinely want to mock the persistence layer, "
                    + "the test is a unit test — rename it so it is no longer an integration test and move it "
                    + "beside the class it tests.")
            .howNotToFix("Do NOT rename the class from FooIT to FooTests just to stop this rule matching while "
                    + "it keeps doing integration work, and do NOT rename the mocked type so it no longer ends "
                    + "in Repository or Dao. Both dodge the matcher and leave the problem in place.")
            .build();

    static final ArchRule NO_MOCKED_REPOS_IN_IT_RULE = noClasses()
            .that().haveSimpleNameEndingWith("IntegrationTest")
            .or().haveSimpleNameEndingWith("IT")
            .should(declareAMockedRepositoryOrDaoField());

    @ArchTest
    public static final ArchRule integrationTestsMustNotMockRepositoriesOrDaos =
            FrozenRules.freeze(NO_MOCKED_REPOS_IN_IT_RULE, NO_MOCKED_REPOS_IN_IT_DOC);

    /**
     * A field violates only when it is <em>both</em> annotated with a mocking annotation and typed
     * as a persistence abstraction. Used with {@code noClasses().should(...)}, so a satisfied event
     * is reported as a violation.
     */
    private static ArchCondition<JavaClass> declareAMockedRepositoryOrDaoField() {
        return new ArchCondition<>("declare a mocked Repository or Dao field") {
            @Override
            public void check(JavaClass testClass, ConditionEvents events) {
                for (JavaField field : testClass.getFields()) {
                    boolean mocked = FORBIDDEN_MOCK_ANNOTATIONS.stream().anyMatch(field::isAnnotatedWith);
                    String typeName = field.getRawType().getSimpleName();
                    boolean persistenceType = typeName.endsWith("Repository") || typeName.endsWith("Dao");
                    if (mocked && persistenceType) {
                        events.add(SimpleConditionEvent.satisfied(field,
                                "Field " + field.getFullName() + " mocks persistence type " + typeName));
                    }
                }
            }
        };
    }

    // ------------------------------------------------------------------- test class naming convention

    static final RuleDoc TEST_NAMING_DOC = RuleDoc.builder()
            .id("test.class-naming-convention")
            .why("Surefire and Failsafe select tests by class name. A class holding @Test methods whose "
                    + "name ends in neither Test nor IT is silently never executed — it looks like coverage "
                    + "in the source tree while proving nothing in CI.")
            .howToFix("Rename the class to end in Test (unit tests, run by Surefire) or IT (integration "
                    + "tests, run by Failsafe).")
            .howNotToFix("Do NOT delete the @Test methods or the class to make this rule pass, and do NOT "
                    + "widen the Surefire include patterns instead of renaming — the convention is what makes "
                    + "the unit/integration split legible.")
            .build();

    static final ArchRule TEST_NAMING_RULE = classes()
            .that().containAnyMethodsThat(
                    describe("annotated with @Test", (JavaMethod method) -> method.isAnnotatedWith(JUNIT_TEST)))
            .should().haveSimpleNameEndingWith("Test")
            .orShould().haveSimpleNameEndingWith("IT");

    @ArchTest
    public static final ArchRule testClassNamingConvention =
            FrozenRules.freeze(TEST_NAMING_RULE, TEST_NAMING_DOC);
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `mvn -B -q -pl llama-rules test -Dtest=TestingRulesTest`

Expected: PASS — 8 tests, 0 failures.

- [ ] **Step 7: Verify the fixtures are not being executed as tests**

Run: `mvn -B -pl llama-rules test 2>&1 | grep -E "Tests run|fixtures"`

Expected: no surefire report mentions `WellNamedTest`, `PlainUnitTest`, or `BadlyNamedTestCase`. If they appear, the exclude from Step 1 did not take effect.

- [ ] **Step 8: Commit**

```bash
git add llama-rules/pom.xml \
        llama-rules/src/main/java/io/github/milczekt1/archrules/groups/TestingRules.java \
        llama-rules/src/test/java/io/github/milczekt1/archrules/groups/TestingRulesTest.java \
        llama-rules/src/test/java/io/github/milczekt1/archrules/fixtures/testing
git commit -m "feat: add TestingRules group guarding IT mocking and test naming"
```

---

## Task 9: `AllCentralRules` aggregator and registry completeness

**Files:**
- Create: `llama-rules/src/main/java/io/github/milczekt1/archrules/groups/AllCentralRules.java`
- Test: `llama-rules/src/test/java/io/github/milczekt1/archrules/groups/RuleRegistryCompletenessTest.java`

**Interfaces:**
- Consumes: `DatabaseRules` (Task 7), `TestingRules` (Task 8), `RuleRegistry` (Task 3).
- Produces: `public final class AllCentralRules` with `@ArchTest public static final ArchTests database` / `testing`, plus `static List<Class<?>> groups()` and `static void loadAll()`. Used by Tasks 11 and 12.

> **Why `loadAll()` exists.** A class literal such as `DatabaseRules.class` does **not** trigger
> static initialisation in Java, so merely listing the group classes would leave the registry
> empty. `loadAll()` uses `Class.forName(name, true, loader)` to force initialisation, which is
> what runs each `FrozenRules.freeze(...)` call and populates `RuleRegistry`. Only tooling that
> needs *every* doc up front (this test, the README test) calls it — at failure time the group
> class is already loaded because the failing rule came from it.

- [ ] **Step 1: Write the failing test**

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/groups/RuleRegistryCompletenessTest.java`:

```java
package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.archrules.RuleDoc;
import io.github.milczekt1.archrules.RuleRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RuleRegistryCompletenessTest {

    @BeforeAll
    static void loadEveryGroup() {
        AllCentralRules.loadAll();
    }

    /** Every {@code @ArchTest ArchRule} field across every group class. */
    private static List<ArchRule> publishedRules() {
        List<ArchRule> rules = new ArrayList<>();
        for (Class<?> group : AllCentralRules.groups()) {
            for (Field field : group.getDeclaredFields()) {
                if (field.isAnnotationPresent(ArchTest.class)
                        && ArchRule.class.isAssignableFrom(field.getType())
                        && Modifier.isStatic(field.getModifiers())) {
                    try {
                        rules.add((ArchRule) field.get(null));
                    } catch (IllegalAccessException e) {
                        throw new AssertionError("rule field must be public: " + field, e);
                    }
                }
            }
        }
        return rules;
    }

    @Test
    void everyPublishedRuleHasARegisteredDoc() {
        for (ArchRule rule : publishedRules()) {
            String description = rule.getDescription();
            assertTrue(RuleRegistry.find(description).isPresent(),
                    "rule description '" + description + "' is not a registered RuleDoc id — the failure"
                            + " formatter would fall back to plain ArchUnit output for it");
        }
    }

    @Test
    void everyRegisteredDocHasUsableGuidance() {
        for (RuleDoc doc : RuleRegistry.all()) {
            assertFalse(doc.why().isBlank(), doc.id() + " has a blank why");
            assertFalse(doc.howToFix().isBlank(), doc.id() + " has a blank howToFix");
        }
    }

    @Test
    void ruleIdsAreUnique() {
        List<String> ids = publishedRules().stream().map(ArchRule::getDescription).toList();
        Set<String> unique = new LinkedHashSet<>(ids);

        assertEquals(ids.size(), unique.size(), "duplicate rule ids among published rules: " + ids);
    }

    @Test
    void publishesExactlyTheSeededFirstCutRules() {
        // Locks the first-cut scope. Adding a rule is a deliberate edit here AND in the README.
        Set<String> ids = new LinkedHashSet<>(publishedRules().stream().map(ArchRule::getDescription).toList());

        assertEquals(Set.of(
                "db.no-spring-transactional-on-classes",
                "db.no-spring-transactional-on-methods",
                "db.no-raw-jdbc-outside-repositories",
                "test.no-mocked-repository-in-integration-test",
                "test.class-naming-convention"), ids);
    }

    @Test
    void aggregatorExposesEveryGroup() {
        assertEquals(List.of(DatabaseRules.class, TestingRules.class), AllCentralRules.groups());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -q -pl llama-rules test -Dtest=RuleRegistryCompletenessTest`

Expected: FAIL — compilation error, `cannot find symbol: class AllCentralRules`.

- [ ] **Step 3: Write the implementation**

Create `llama-rules/src/main/java/io/github/milczekt1/archrules/groups/AllCentralRules.java`:

```java
package io.github.milczekt1.archrules.groups;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import java.util.List;

/**
 * Opt into every central rule group with a single field:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.acme", importOptions = ImportOption.DoNotIncludeJars.class)
 * class CentralArchitectureTest {
 *     @ArchTest
 *     static final ArchTests all = ArchTests.in(AllCentralRules.class);
 * }
 * }</pre>
 *
 * <p>Growth path — add a group class here once it is seeded:
 * {@code Java17Rules}, {@code JakartaMigrationRules}, {@code SpringRules}.
 */
public final class AllCentralRules {

    @ArchTest
    public static final ArchTests database = ArchTests.in(DatabaseRules.class);

    @ArchTest
    public static final ArchTests testing = ArchTests.in(TestingRules.class);

    private AllCentralRules() {
    }

    /** Every seeded group class, in documentation order. */
    public static List<Class<?>> groups() {
        return List.of(DatabaseRules.class, TestingRules.class);
    }

    /**
     * Forces static initialisation of every group class, populating
     * {@code RuleRegistry} with all their docs.
     *
     * <p>Needed because a class literal alone does not initialise a class — without this, tooling
     * that wants every doc up front (completeness checks, README generation) would see an empty
     * registry.
     */
    public static void loadAll() {
        for (Class<?> group : groups()) {
            try {
                Class.forName(group.getName(), true, group.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Could not load rule group " + group.getName(), e);
            }
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B -q -pl llama-rules test -Dtest=RuleRegistryCompletenessTest`

Expected: PASS — 5 tests, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add llama-rules/src/main/java/io/github/milczekt1/archrules/groups/AllCentralRules.java \
        llama-rules/src/test/java/io/github/milczekt1/archrules/groups/RuleRegistryCompletenessTest.java
git commit -m "feat: add AllCentralRules aggregator with registry completeness guard"
```

---

## Task 10: End-to-end freezing and failure-message behaviour

**Files:**
- Test: `llama-rules/src/test/java/io/github/milczekt1/archrules/groups/FreezingBehaviourTest.java`
- Create (fixture): `llama-rules/src/test/java/io/github/milczekt1/archrules/fixtures/database/service/SecondAnnotatedService.java`

**Interfaces:**
- Consumes: everything from Tasks 2–9.
- Produces: no production code. This is the task that proves the three moving parts work *together*, and it is the only place `AgentFriendlyFailureDisplayFormat.formatFailure` (as opposed to its seams) is exercised — `FailureMessages` cannot be constructed in a unit test.

> **How "a new violation" is simulated.** Classes cannot be added at runtime, so the test seeds the
> store against a *narrow* set of imported classes and then re-evaluates the same rule against a
> *wider* set. The second class is genuinely new to the store, which is exactly the situation a
> consumer hits when someone commits a new violation.

- [ ] **Step 1: Write the fixture**

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/fixtures/database/service/SecondAnnotatedService.java`:

```java
package io.github.milczekt1.archrules.fixtures.database.service;

import org.springframework.transaction.annotation.Transactional;

/** A second class-level violation, used to simulate a newly introduced violation. */
@Transactional
public class SecondAnnotatedService {
    public void doWork() {
    }
}
```

- [ ] **Step 2: Write the failing test**

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/groups/FreezingBehaviourTest.java`:

```java
// Lives in the `groups` package on purpose: it reads DatabaseRules' package-private
// raw-rule and doc constants.
package io.github.milczekt1.archrules.groups;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import io.github.milczekt1.archrules.FrozenRules;
import io.github.milczekt1.archrules.format.AntiFixPolicy;
import io.github.milczekt1.archrules.fixtures.database.service.AnnotatedService;
import io.github.milczekt1.archrules.fixtures.database.service.CleanService;
import io.github.milczekt1.archrules.fixtures.database.service.SecondAnnotatedService;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FreezingBehaviourTest {

    @TempDir
    Path store;

    private static final JavaClasses PRE_EXISTING =
            new ClassFileImporter().importClasses(AnnotatedService.class, CleanService.class);

    private static final JavaClasses WITH_NEW_VIOLATION = new ClassFileImporter()
            .importClasses(AnnotatedService.class, CleanService.class, SecondAnnotatedService.class);

    @BeforeEach
    void useTemporaryStoreAndTheAgentFriendlyFormatter() {
        ArchConfiguration.get().setProperty("freeze.store.default.path", store.toString());
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreCreation", "true");
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreUpdate", "true");
        ArchConfiguration.get().setProperty("failureDisplayFormat",
                "io.github.milczekt1.archrules.format.AgentFriendlyFailureDisplayFormat");
    }

    @AfterEach
    void resetConfiguration() {
        ArchConfiguration.get().reset();
    }

    /** A fresh frozen instance per call; the shared temp store is what carries state. */
    private static ArchRule frozenRule() {
        return FrozenRules.freeze(DatabaseRules.NO_TX_ON_CLASSES_RULE, DatabaseRules.NO_TX_ON_CLASSES_DOC);
    }

    @Test
    void adoptionDoesNotBlock_firstRunSeedsExistingViolationsAndPasses() {
        assertDoesNotThrow(() -> frozenRule().check(PRE_EXISTING));
    }

    @Test
    void onlyNewViolationsFail() {
        frozenRule().check(PRE_EXISTING); // seed

        AssertionError failure = assertThrows(AssertionError.class,
                () -> frozenRule().check(WITH_NEW_VIOLATION));

        assertTrue(failure.getMessage().contains("SecondAnnotatedService"),
                "the new violation must be reported: " + failure.getMessage());
        // NB: ".service.AnnotatedService>" — a bare "AnnotatedService>" is also a substring of
        // "SecondAnnotatedService>", so the package prefix is what makes this discriminate.
        assertFalse(failure.getMessage().contains(".service.AnnotatedService>"),
                "the frozen pre-existing violation must stay silent: " + failure.getMessage());
    }

    @Test
    void reRunningWithoutNewViolationsStaysGreen() {
        frozenRule().check(PRE_EXISTING);

        assertDoesNotThrow(() -> frozenRule().check(PRE_EXISTING));
    }

    @Test
    void theFailureTeachesWhyAndHowToFix() {
        frozenRule().check(PRE_EXISTING);

        AssertionError failure = assertThrows(AssertionError.class,
                () -> frozenRule().check(WITH_NEW_VIOLATION));
        String message = failure.getMessage();

        assertTrue(message.contains("Architecture Violation [db.no-spring-transactional-on-classes]"), message);
        assertTrue(message.contains("WHY:"), message);
        assertTrue(message.contains(DatabaseRules.NO_TX_ON_CLASSES_DOC.why()), message);
        assertTrue(message.contains("HOW TO FIX:"), message);
        assertTrue(message.contains(DatabaseRules.NO_TX_ON_CLASSES_DOC.howToFix()), message);
        assertTrue(message.contains("HOW NOT TO FIX (this rule):"), message);
        assertTrue(message.contains("Offending locations:"), message);
    }

    @Test
    void theFailureCarriesTheWholeGlobalAntiFixPolicy() {
        frozenRule().check(PRE_EXISTING);

        AssertionError failure = assertThrows(AssertionError.class,
                () -> frozenRule().check(WITH_NEW_VIOLATION));

        assertTrue(failure.getMessage().contains("HOW NOT TO FIX (always):"), failure.getMessage());
        for (String clause : AntiFixPolicy.clauses()) {
            assertTrue(failure.getMessage().contains(clause), "missing clause: " + clause);
        }
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn -B -q -pl llama-rules test -Dtest=FreezingBehaviourTest`

Expected: FAIL — compilation error on the missing `SecondAnnotatedService` import if Step 1 was skipped; otherwise the assertions in `theFailureTeachesWhyAndHowToFix` fail if the formatter is not wired.

- [ ] **Step 4: Make it pass**

No production code should be needed — Tasks 2–9 already provide everything. If a test fails, fix the *cause*, do not weaken the assertion:

- Frozen violation still reported → `.as(doc.id())` is not being applied before freezing (Task 6).
- Plain ArchUnit message instead of `WHY:` → the `failureDisplayFormat` property name is wrong, or `AgentFriendlyFailureDisplayFormat` is not public / has no no-arg constructor.
- The `.service.AnnotatedService>` assertion trips → ArchUnit renders `Class <...AnnotatedService>`, and the package prefix plus trailing `>` is what stops it matching the `SecondAnnotatedService` line. Do not shorten it.

- [ ] **Step 5: Run the full module suite**

Run: `mvn -B -pl llama-rules test`

Expected: PASS — all test classes green, no fixture class executed as a test.

- [ ] **Step 6: Commit**

```bash
git add llama-rules/src/test/java/io/github/milczekt1/archrules/groups/FreezingBehaviourTest.java \
        llama-rules/src/test/java/io/github/milczekt1/archrules/fixtures/database/service/SecondAnnotatedService.java
git commit -m "test: cover freeze seeding, new-violation failure and agent-friendly output end to end"
```

---

## Task 11: `examples/consumer-junit5` — the integration smoke test

**Files:**
- Modify: `pom.xml` (uncomment the `examples/consumer-junit5` module from Task 1)
- Create: `examples/consumer-junit5/pom.xml`
- Create: `examples/consumer-junit5/src/main/java/com/example/consumer/repository/CustomerRepository.java`
- Create: `examples/consumer-junit5/src/main/java/com/example/consumer/service/ReportService.java`
- Create: `examples/consumer-junit5/src/main/java/com/example/consumer/service/GreetingService.java`
- Create: `examples/consumer-junit5/src/test/java/com/example/consumer/CentralArchitectureTest.java`
- Create: `examples/consumer-junit5/src/test/resources/archunit.properties`
- Create: `examples/consumer-junit5/src/test/resources/archunit/frozen/` (generated on first run, then committed)

**Interfaces:**
- Consumes: the published `io.github.milczekt1:llama-rules` artifact (resolved from the reactor), `AllCentralRules` (Task 9), `AgentFriendlyFailureDisplayFormat` (Task 5).
- Produces: a working reference wiring that doubles as CI proof that one test-scoped dependency plus one thin test class is genuinely all a consumer needs.

> **Deliberately zero extra dependencies.** The example's seeded violation is raw `java.sql` usage
> from the JDK, so the example proves the "one test-scoped dependency" claim literally — no Spring,
> no Mockito. This also exercises the transitive `archunit-junit5` engine inheritance.

- [ ] **Step 1: Re-enable the module**

In the root `pom.xml`, uncomment:

```xml
    <module>examples/consumer-junit5</module>
```

- [ ] **Step 2: Create the example pom**

Create `examples/consumer-junit5/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>io.github.milczekt1</groupId>
    <artifactId>llama-rules-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../../pom.xml</relativePath>
  </parent>

  <artifactId>llama-rules-consumer-example</artifactId>
  <packaging>jar</packaging>

  <name>LLamaRules Example Consumer (JUnit 5)</name>
  <description>Reference wiring: one test-scoped dependency plus one thin test class.</description>

  <dependencies>
    <!-- THE ONLY DEPENDENCY A CONSUMER NEEDS.
         archunit-junit5 (and its engine) arrive transitively. -->
    <dependency>
      <groupId>io.github.milczekt1</groupId>
      <artifactId>llama-rules</artifactId>
      <version>${project.version}</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-deploy-plugin</artifactId>
        <configuration>
          <skip>true</skip>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 3: Create the example sources**

`examples/consumer-junit5/src/main/java/com/example/consumer/repository/CustomerRepository.java` — compliant:

```java
package com.example.consumer.repository;

import java.sql.Connection;
import java.sql.SQLException;

/** JDBC lives here, in the persistence layer. Compliant. */
public class CustomerRepository {
    public void loadAll(Connection connection) throws SQLException {
        connection.createStatement().execute("SELECT * FROM customers");
    }
}
```

`examples/consumer-junit5/src/main/java/com/example/consumer/service/ReportService.java` — the deliberate, *frozen* violation:

```java
package com.example.consumer.service;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Pre-existing technical debt: raw JDBC in a service package.
 *
 * <p>Deliberately left in place. It is seeded into the committed freeze store on first run, which
 * demonstrates the "freeze, don't block" principle — adopting the rules does not force a
 * repository-wide cleanup before the build can go green.
 */
public class ReportService {
    public void render(Connection connection) throws SQLException {
        connection.createStatement().execute("SELECT count(*) FROM orders");
    }
}
```

`examples/consumer-junit5/src/main/java/com/example/consumer/service/GreetingService.java` — clean:

```java
package com.example.consumer.service;

public class GreetingService {
    public String greet(String name) {
        return "Hello, " + name;
    }
}
```

- [ ] **Step 4: Create the thin consumer test class**

`examples/consumer-junit5/src/test/java/com/example/consumer/CentralArchitectureTest.java`:

```java
package com.example.consumer;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import io.github.milczekt1.archrules.groups.AllCentralRules;
import io.github.milczekt1.archrules.groups.DatabaseRules;
import io.github.milczekt1.archrules.groups.TestingRules;

/**
 * The entire consumer-side wiring. No rule logic lives here.
 *
 * <p>Run granularities: the whole class, one group node, or a single rule leaf.
 *
 * <p>Note the absence of {@code ImportOption.DoNotIncludeTests}: {@link TestingRules} inspects test
 * classes, and excluding them would make those rules pass vacuously.
 */
@AnalyzeClasses(packages = "com.example.consumer", importOptions = ImportOption.DoNotIncludeJars.class)
class CentralArchitectureTest {

    @ArchTest
    static final ArchTests all = ArchTests.in(AllCentralRules.class);

    // Equivalent, if you prefer to opt in group by group:
    // @ArchTest static final ArchTests database = ArchTests.in(DatabaseRules.class);
    // @ArchTest static final ArchTests testing  = ArchTests.in(TestingRules.class);
}
```

> Remove the two unused imports (`DatabaseRules`, `TestingRules`) if your build treats unused
> imports as errors; they exist only to make the commented alternative copy-pasteable.

- [ ] **Step 5: Configure ArchUnit for the consumer**

`examples/consumer-junit5/src/test/resources/archunit.properties`:

```properties
# Freeze store: seeded on first run, then committed. Only NEW violations fail the build.
freeze.store.default.allowStoreCreation=true
freeze.store.default.path=src/test/resources/archunit/frozen

# Activates WHY / HOW TO FIX / anti-fix policy on failure. This is a GLOBAL per-run setting;
# the formatter falls back to ArchUnit's standard output for your own non-framework rules.
failureDisplayFormat=io.github.milczekt1.archrules.format.AgentFriendlyFailureDisplayFormat
```

- [ ] **Step 6: Seed the freeze store**

Run: `mvn -B test`

(Build the whole reactor: the example resolves `llama-rules` from the reactor, so no `install` is
needed. Selecting the example alone requires `-pl examples/consumer-junit5 -am`.)

Expected: PASS. `ReportService`'s raw-JDBC violation is seeded rather than failing the build.

Verify the store was written with the short id as the key:

```bash
cat examples/consumer-junit5/src/test/resources/archunit/frozen/stored.rules
```

Expected: a line `db.no-raw-jdbc-outside-repositories=<uuid>` (plus entries for the other rules).

- [ ] **Step 7: Prove a NEW violation fails with the full message**

This is the design's manual verification step. Temporarily add a second violating service:

```bash
cat > examples/consumer-junit5/src/main/java/com/example/consumer/service/AuditService.java <<'EOF'
package com.example.consumer.service;

import java.sql.Connection;
import java.sql.SQLException;

public class AuditService {
    public void write(Connection connection) throws SQLException {
        connection.createStatement().execute("INSERT INTO audit VALUES (1)");
    }
}
EOF
mvn -B test -pl examples/consumer-junit5 -am
```

Expected: **FAIL**, and the output contains `Architecture Violation [db.no-raw-jdbc-outside-repositories]`, `WHY:`, `HOW TO FIX:`, `HOW NOT TO FIX (always):`, and names `AuditService` but **not** `ReportService`.

Then remove it and confirm green again:

```bash
rm examples/consumer-junit5/src/main/java/com/example/consumer/service/AuditService.java
git checkout -- examples/consumer-junit5/src/test/resources/archunit/frozen
mvn -B test -pl examples/consumer-junit5 -am
```

Expected: PASS.

> `git checkout` on the store matters: `allowStoreUpdate` defaults to true, so a failing run may
> have rewritten it. Never commit a store mutated by a *failing* run.

- [ ] **Step 8: Run the whole reactor**

Run: `mvn -B verify`

Expected: PASS — both modules green.

- [ ] **Step 9: Commit**

```bash
git add pom.xml examples
git commit -m "docs: add example JUnit 5 consumer with committed freeze store"
```

---

## Task 12: README rules table with a docs-drift guard

**Files:**
- Modify: `README.md`
- Test: `llama-rules/src/test/java/io/github/milczekt1/archrules/ReadmeRulesTableTest.java`

**Interfaces:**
- Consumes: `RuleRegistry.all()` (Task 3), `AllCentralRules.loadAll()` (Task 9).
- Produces: a README whose rules table cannot drift from the code — a missing row *or* a stale row fails the build.

- [ ] **Step 1: Write the failing test**

Create `llama-rules/src/test/java/io/github/milczekt1/archrules/ReadmeRulesTableTest.java`:

```java
package io.github.milczekt1.archrules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.milczekt1.archrules.groups.AllCentralRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The README rules table is documentation that must not drift from the registry. */
class ReadmeRulesTableTest {

    /** Matches a leading table cell holding a backticked rule id, e.g. {@code | `db.foo` |}. */
    private static final Pattern ROW = Pattern.compile("^\\|\\s*`([a-z0-9]+(?:\\.[a-z0-9-]+)+)`\\s*\\|", Pattern.MULTILINE);

    private static String readme;

    @BeforeAll
    static void loadEverything() throws IOException {
        AllCentralRules.loadAll();
        readme = Files.readString(readmePath());
    }

    /** Surefire runs with basedir = the module directory, so the repo root is one level up. */
    private static Path readmePath() {
        Path fromModule = Path.of("..", "README.md");
        return Files.exists(fromModule) ? fromModule : Path.of("README.md");
    }

    private static Set<String> documentedIds() {
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = ROW.matcher(readme);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    @Test
    void everyRuleIsDocumented() {
        Set<String> registered = new LinkedHashSet<>(RuleRegistry.all().stream().map(RuleDoc::id).toList());

        assertTrue(documentedIds().containsAll(registered),
                "README is missing rows for: " + minus(registered, documentedIds()));
    }

    @Test
    void noStaleRowsSurviveARemovedRule() {
        Set<String> registered = new LinkedHashSet<>(RuleRegistry.all().stream().map(RuleDoc::id).toList());

        assertTrue(registered.containsAll(documentedIds()),
                "README documents rules that no longer exist: " + minus(documentedIds(), registered));
    }

    @Test
    void tableAndRegistryMatchExactly() {
        Set<String> registered = new LinkedHashSet<>(RuleRegistry.all().stream().map(RuleDoc::id).toList());

        assertEquals(registered.stream().sorted().toList(), documentedIds().stream().sorted().toList());
    }

    @Test
    void readmeExplainsTheConsumerWiring() {
        assertTrue(readme.contains("ArchTests.in(AllCentralRules.class)"), "README must show the wiring");
        assertTrue(readme.contains("failureDisplayFormat"), "README must show how to enable rich failures");
        assertTrue(readme.contains("DoNotIncludeTests"),
                "README must warn against excluding test classes");
    }

    private static Set<String> minus(Set<String> a, Set<String> b) {
        Set<String> result = new LinkedHashSet<>(a);
        result.removeAll(b);
        return result;
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -B -q -pl llama-rules test -Dtest=ReadmeRulesTableTest`

Expected: FAIL — `README is missing rows for: [db.no-raw-jdbc-outside-repositories, ...]`.

- [ ] **Step 3: Write the README**

Replace `README.md`:

````markdown
# LLamaRules

Library of rules that guards the code.

Centralized [ArchUnit](https://www.archunit.org/) rules you write once and enforce across every
Java module: one test-scoped dependency, one thin test class. Every rule is frozen — adopting the
library records existing violations instead of blocking your build — and every failure explains why
the rule exists, how to fix it, and how *not* to fake a fix.

## Install

```xml
<dependency>
  <groupId>io.github.milczekt1</groupId>
  <artifactId>llama-rules</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

That is the only dependency you need — the `archunit-junit5` engine arrives transitively.

## Wire it up

`src/test/java/com/acme/CentralArchitectureTest.java`:

```java
@AnalyzeClasses(packages = "com.acme", importOptions = ImportOption.DoNotIncludeJars.class)
class CentralArchitectureTest {
    @ArchTest
    static final ArchTests all = ArchTests.in(AllCentralRules.class);
}
```

Opt in group by group instead with `ArchTests.in(DatabaseRules.class)` / `ArchTests.in(TestingRules.class)`.

> Do **not** add `ImportOption.DoNotIncludeTests`. The testing rules inspect your test classes;
> excluding them makes those rules pass vacuously.

`src/test/resources/archunit.properties`:

```properties
freeze.store.default.allowStoreCreation=true
freeze.store.default.path=src/test/resources/archunit/frozen

failureDisplayFormat=io.github.milczekt1.archrules.format.AgentFriendlyFailureDisplayFormat
```

Run the build once, then **commit** `src/test/resources/archunit/frozen/`. Existing violations are
now recorded as debt; only new ones fail.

`failureDisplayFormat` is a global per-run setting, but the formatter falls back to ArchUnit's
standard output for any rule it does not own, so your own ArchUnit tests are unaffected.

## Rules

| Rule id | Group | What it enforces |
|---|---|---|
| `db.no-spring-transactional-on-classes` | `DatabaseRules` | No class annotated `@org.springframework.transaction.annotation.Transactional`. |
| `db.no-spring-transactional-on-methods` | `DatabaseRules` | No method annotated `@Transactional` either — it is banned in every position. |
| `db.no-raw-jdbc-outside-repositories` | `DatabaseRules` | `java.sql` / `javax.sql` / `JdbcTemplate` only inside `..repository..`, `..repositories..`, `..dao..`, `..jdbc..`, `..persistence..`. |
| `test.no-mocked-repository-in-integration-test` | `TestingRules` | A `*IntegrationTest` / `*IT` class must not declare a mocked (`@Mock`, `@MockitoBean`, `@MockBean`) field whose type ends in `Repository` or `Dao`. |
| `test.class-naming-convention` | `TestingRules` | A class holding `@Test` methods must end in `Test` or `IT`, so Surefire/Failsafe actually run it. |

This table is verified against the code by `ReadmeRulesTableTest` — a missing or stale row fails the build.

## Run granularity

The wiring produces a real JUnit test tree, so you can run the whole suite, one group node, or a
single rule leaf from your IDE gutter or via `-Dtest=`.

## Growth path

`Java17Rules`, `JakartaMigrationRules`, and `SpringRules` are intentionally **not** in the first
cut. To add a group: create the class under `groups/`, give each rule a `RuleDoc` with a unique id,
wrap it with `FrozenRules.freeze(...)`, register the class in `AllCentralRules.groups()`, and add a
row above.

> **Rule ids are freeze-store keys.** Changing an id orphans every consumer's frozen entry, so
> treat it as a breaking change.

## License

MIT
````

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -B -q -pl llama-rules test -Dtest=ReadmeRulesTableTest`

Expected: PASS — 4 tests, 0 failures.

- [ ] **Step 5: Full verification**

Run: `mvn -B verify`

Expected: PASS — every module, every test.

- [ ] **Step 6: Commit**

```bash
git add README.md llama-rules/src/test/java/io/github/milczekt1/archrules/ReadmeRulesTableTest.java
git commit -m "docs: add rules table with drift guard"
```

---

## Final verification

Run these in order from the repo root; all must pass before the work is considered done.

1. `mvn -B verify` — both modules green.
2. `cat examples/consumer-junit5/src/test/resources/archunit/frozen/stored.rules` — keys are short rule ids, not English sentences.
3. `git status --porcelain` — clean. In particular the freeze store must not be modified by a passing run.
4. Task 11 Step 7's manual check — a new violation fails with the full `WHY` / `HOW TO FIX` / anti-fix message; removing it returns the build to green.

## Deferred, by design

Named here so nobody mistakes them for oversights: `Java17Rules`, `JakartaMigrationRules`,
`SpringRules`; Gradle consumer examples; detecting inline `Mockito.mock(XRepository.class)` calls
in method bodies (ArchUnit cannot reliably resolve the mocked type); rollout to specific consumer
repositories; and any script that mutates `CLAUDE.md` — the failing test is the enforcement
mechanism.
