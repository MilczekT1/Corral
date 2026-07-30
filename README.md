# LLamaRules

Library of rules that guards the code.

Centralized [ArchUnit](https://www.archunit.org/) rules you write once and enforce across every
Java module: one test-scoped dependency, one thin test class. Every rule is frozen — adopting the
library records existing violations instead of blocking your build — and every failure explains why
the rule exists, how to fix it, and how *not* to fake a fix.

## Install

One dependency — the `archunit-junit5` engine arrives transitively:

```xml
<dependency>
  <groupId>io.github.milczekt1</groupId>
  <artifactId>llama-rules</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

`0.1.0-SNAPSHOT` is a snapshot; no release has been cut yet.

The artifact is published to GitHub Packages, which Maven does not know about by default, so add the
repository too:

```xml
<repositories>
  <repository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/MilczekT1/LLamaRules</url>
    <snapshots><enabled>true</enabled></snapshots>
  </repository>
</repositories>
```

GitHub Packages requires authentication even for public reads, so add a matching server to your
`~/.m2/settings.xml` — the `<id>` must equal the repository `<id>` above:

```xml
<servers>
  <server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_GITHUB_TOKEN</password>  <!-- classic PAT with read:packages -->
  </server>
</servers>
```

Without both pieces the build fails with `Could not find artifact io.github.milczekt1:llama-rules`.
In CI, use a secret rather than a checked-in token (`${env.GITHUB_TOKEN}` interpolates in
`settings.xml`).

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
freeze.store.default.path=src/test/resources/archunit/frozen

failureDisplayFormat=io.github.milczekt1.archrules.format.AgentFriendlyFailureDisplayFormat
```

Then seed the freeze store **once**, out of band, and commit it:

```bash
mvn test -Darchunit.freeze.store.default.allowStoreCreation=true
git add src/test/resources/archunit/frozen && git commit -m "chore: freeze existing violations"
```

Existing violations are now recorded as debt; only new ones fail.

> **Do not put `freeze.store.default.allowStoreCreation=true` in `archunit.properties`.** ArchUnit
> defaults it to `false`, and that default is the only thing separating "the store is missing" from
> "silently freeze everything and pass". Pinned to `true`, a store that was never committed, got
> gitignored, was lost to a shallow checkout, or is simply not where the working directory says it is
> gives you a green build with every violation re-frozen and no signal at all. Left at its default,
> the same situation fails loudly with
> `Creating new violation store is disabled (enable by configuration freeze.store.default.allowStoreCreation=true)`.
> ArchUnit merges any `archunit.`-prefixed system property, which is why the one-off override above
> works without editing the committed file.

> **`freeze.store.default.path` is resolved against the JVM's working directory**, not the
> classpath. Maven sets it to the module directory, so the relative path above works for `mvn test`;
> an IDE run configuration with a different working directory will not find the store.

`failureDisplayFormat` is a global per-run setting, but the formatter falls back to ArchUnit's
standard output for any rule it does not own, so your own ArchUnit tests are unaffected.

## Rules

| Rule id | Group | What it enforces |
|---|---|---|
| `test.no-mocked-repository-in-integration-test` | `TestingRules` | A `*IntegrationTest` / `*IT` class must not declare a mocked (`@Mock`, `@MockitoBean`, `@MockBean`) field whose type ends in `Repository` or `Dao`. |
| `test.class-naming-convention` | `TestingRules` | A top-level class holding JUnit test methods (`@Test`, `@ParameterizedTest`, `@RepeatedTest`, `@TestFactory`, `@TestTemplate`) must end in `Test` or `IT`, so Surefire/Failsafe actually run it. Nested classes — including JUnit 5 `@Nested` groups — are exempt: they run through their enclosing class. |

This table is verified against the code by `ReadmeRulesTableTest` — a missing or stale row fails the build.

## Run granularity

The wiring produces a real JUnit test tree, so you can run the whole suite, one group node, or a
single rule leaf from your IDE gutter or via `-Dtest=`.

## Growth path

`Java17Rules`, `JakartaMigrationRules`, and `SpringRules` are intentionally **not** in the first
cut. Adding a group means all nine steps below, in order. Skipping step 5 in particular leaves the
build green while **no consumer ever evaluates the new group** — `ArchTests.in(AllCentralRules.class)`
descends into `@ArchTest` fields only, never into `groups()`.

1. Create the group class under `groups/`.
2. Give each rule a `RuleDoc` with a unique id (`<group>.<kebab-case-rule>`).
3. Wrap each raw rule with `FrozenRules.freeze(rawRule, doc)`.
4. Expose it as a public `@ArchTest ArchRule` field on the group class, keeping the package-private
   raw `*_RULE` constant for unit testing.
5. Add an `@ArchTest ArchTests` field (`public static final`) for the group on `AllCentralRules` —
   this, and only this, is what consumers run.
6. Add the group class to `AllCentralRules.groups()` — what the completeness and README tooling
   reads. `AllCentralRulesTest` fails if steps 5 and 6 disagree.
7. Extend the expected id set in `RuleRegistryCompletenessTest.publishesExactlyTheSeededFirstCutRules`.
8. Add a row to the [Rules](#rules) table above; `ReadmeRulesTableTest` fails otherwise.
9. Add fixtures under `src/test/java/.../fixtures/` and a rule test asserting both what the rule
   flags and what it must leave alone, plus a pairing test (see `*FrozenFieldsTest`) so the public
   frozen field is pinned to its own raw rule.

> **Rule ids are freeze-store keys.** Changing an id orphans every consumer's frozen entry, so
> treat it as a breaking change.

## Contributing

This module uses [Lombok](https://projectlombok.org/). Install your IDE's Lombok plugin, or the IDE
will report errors on generated members that `mvn verify` compiles cleanly.

Lombok is wired through `annotationProcessorPaths` in the root `pom.xml`, not just as a dependency —
JDK 23+ ignores annotation processors that are only on the classpath, and would otherwise generate
nothing while still reporting a successful build. `LombokWiringTest` guards against that; run it
with `clean`, since a stale `target/classes` will mask the failure.

## License

MIT
