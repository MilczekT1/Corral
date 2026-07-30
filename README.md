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
