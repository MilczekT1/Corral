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

Opt in group by group instead with `ArchTests.in(TestingRules.class)`.

> Do **not** add `ImportOption.DoNotIncludeTests`. The testing rules inspect your test classes;
> excluding them makes those rules pass vacuously.

`src/test/resources/archunit.properties`:

```properties
freeze.store.default.path=src/test/resources/archunit/frozen

failureDisplayFormat=io.github.milczekt1.archrules.format.AgentFriendlyFailureDisplayFormat

# Optional. Keeps stored.rules complete but writes no file for a rule with zero violations,
# so the committed store carries no empty files. See "Empty violation files" below.
freeze.store=io.github.milczekt1.archrules.freeze.EmptyOmittingViolationStore
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

### Empty violation files

A rule that is already clean in your codebase still gets frozen — ArchUnit records the rule in
`stored.rules` and writes it an empty violation file. That index entry is what keeps the rule
enforced (a rule the store does *not* contain is seeded-and-passed on its next run, so its first
real violation would be absorbed as debt instead of failing). The empty file, however, is pure
noise in a commit.

`freeze.store=io.github.milczekt1.archrules.freeze.EmptyOmittingViolationStore` keeps the index
entry and drops only the file. Three things to know before enabling it:

- **It is opt-in.** Leave the line out and you get ArchUnit's stock `TextFileBasedViolationStore`,
  empty files included. Nothing else in this library depends on it.
- **It is global per run.** ArchUnit reads `freeze.store` once and uses that store for *every*
  `FreezingArchRule` in the run, including your own frozen rules — not just this library's. The
  behaviour change is narrow (an empty file is not written, or is deleted when a rule becomes
  clean) and the index entry is always preserved, so a frozen rule of yours stays exactly as
  enforced as it was.
- **It does not clean up retroactively.** Switching it on in a project that already has empty files
  committed leaves them there. `FreezingArchRule` only writes to the store when it has something to
  change: for a rule that is contained, clean, and stays clean, no violation is solved and no `save`
  is ever called, so the store never gets a chance to remove the file. Clear them once with

  ```bash
  mvn test -Darchunit.freeze.refreeze=true
  ```

  (or just delete the empty files by hand) and commit the result. Run it from a clean tree and
  review the diff: `refreeze` re-records *every* rule, so any violation currently failing the build
  gets absorbed as debt too. New rules seeded after that point never produce an empty file in the
  first place.

## Rules

| Rule id | Group | What it enforces |
|---|---|---|
| `test.no-mocked-repository-in-integration-test` | `TestingRules` | A `*IntegrationTest` / `*IT` class must not declare a mocked (`@Mock`, `@MockitoBean`, `@MockBean`) field whose type ends in `Repository` or `Dao`. |
| `test.class-naming-convention` | `TestingRules` | A top-level class holding JUnit test methods (`@Test`, `@ParameterizedTest`, `@RepeatedTest`, `@TestFactory`, `@TestTemplate`) must end in `Test` or `IT`, so Surefire/Failsafe actually run it. Nested classes — including JUnit 5 `@Nested` groups — are exempt: they run through their enclosing class. |

This table is maintained by hand. Nothing in the build checks it, so a new rule can ship
undocumented — adding the row is a step in the growth path below, not something a test will remind
you about.

## Run granularity

The wiring produces a real JUnit test tree, so you can run the whole suite, one group node, or a
single rule leaf from your IDE gutter or via `-Dtest=`.

## Growth path

### How the tree is laid out

One rule, one class. A rule class lives under `rules/<topic>/` and owns everything about that rule;
a group under `groups/` is a thin wrapper that composes rule classes, and `AllCentralRules` is a
group of groups. `ArchTests.in(X.class)` descends into `X`'s `@ArchTest` fields, so the same shape
nests to any depth:

```
groups/AllCentralRules      @ArchTest ArchTests testing   ->  groups/TestingRules
groups/TestingRules         @ArchTest ArchTests ...       ->  rules/testing/TestClassNamingConvention
rules/testing/…             @ArchTest ArchRule  rule      ->  the frozen rule a consumer evaluates
```

Every node states its membership **twice**: as `@ArchTest` fields (what
`ArchTests.in(...)` actually descends into — what consumers run) and in a static `members()` (what
the tooling reads). `GroupMembershipTest` walks the whole tree from `AllCentralRules` and fails on
any node where the two disagree, or on any group that has `@ArchTest ArchTests` fields but no
`members()`. It is recursive on purpose: a new group is guarded the moment it becomes reachable, so
there is no per-group test to remember to write.

### Adding a rule to an existing group

1. Create `rules/<topic>/<RuleName>.java` — public final class, private constructor. It holds three
   members (see `TestClassNamingConvention` for a worked example):
   - `static final RuleDoc DOC` — `RuleDoc.builder().id(...).why(...).howToFix(...)` plus the
     optional `.howNotToFix(...)`. The id is `<topic>.<kebab-case-rule>` and must match
     `^[a-z0-9]+(\.[a-z0-9-]+)+$`.
   - `static final ArchRule RULE` — the raw rule, package-private. Tests exercise *this*: the public
     field below is frozen, so it seeds and passes, which would make rule-correctness tests
     meaningless.
   - `@ArchTest public static final ArchRule rule = FrozenRules.freeze(RULE, DOC);` — the field
     consumers evaluate. `freeze` registers the doc, renames the rule to the doc id (that name is
     the freeze-store key), and allows an empty `should`.
2. In the group class, add the rule class to `MEMBERS` **and** give it its own
   `@ArchTest public static final ArchTests` field. Both, always — `GroupMembershipTest` fails
   otherwise. Add only the `members()` entry and the rule is documented and completeness-checked
   but **never evaluated by any consumer**.
3. Add fixtures under `src/test/java/.../fixtures/<topic>/` — at least one class the rule must flag
   and one it must leave alone. (Surefire excludes `**/fixtures/**`, so fixtures named `*Test` or
   `*IT` are not executed as tests.) Then write `rules/<topic>/<RuleName>Test.java` against the raw
   `RULE`, asserting both directions.
4. Add a pairing test to the group's `<Group>FrozenFieldsTest` via
   `FrozenFieldStores.assertFreezes(<RuleClass>.rule, <RuleClass>.RULE, <RuleClass>.DOC, …)` and
   bump its `PAIRING_TESTS` count. This catches a copy-paste slip such as
   `FrozenRules.freeze(A_RULE, B_DOC)`, which every other test in the suite would pass.
5. Extend the expected id set in `AllCentralRulesTest.ruleDiscoveryDescendsThroughNestedGroups`.
6. Add a row to the [Rules](#rules) table above. Nothing enforces this — it is on you.

### Adding a group

`Java17Rules`, `JakartaMigrationRules`, and `SpringRules` are intentionally **not** in the first
cut. On top of the rule steps above:

1. Create `groups/<Topic>Rules.java` — public final class, private constructor, holding a private
   `MEMBERS` list, one `@ArchTest public static final ArchTests` field per member, and
   `public static List<Class<?>> members()` returning `MEMBERS`. Copy `TestingRules`.
2. Add the group to `AllCentralRules.MEMBERS` **and** give it an `@ArchTest ArchTests` field there.
   Skipping the field is the dangerous half: the build stays green while **no consumer ever
   evaluates the new group**, because `ArchTests.in(AllCentralRules.class)` descends into
   `@ArchTest` fields only, never into `members()`. `GroupMembershipTest` fails if the two diverge.
3. Update `AllCentralRulesTest.groupsAreListedInDocumentationOrder`, which pins the group order.
4. Create the group's `<Group>FrozenFieldsTest` (copy `TestingRulesFrozenFieldsTest`) — it is the
   home for step 4 above.

You do **not** need to write a membership guard test for the new group. `GroupMembershipTest`
already covers it, by construction.

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
