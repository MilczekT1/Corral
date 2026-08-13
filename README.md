# LLamaGuard

Centralized [ArchUnit](https://www.archunit.org/) rules you write once and enforce everywhere. One
test-scoped dependency, one thin test class, and your architecture rules stop being prose in a wiki.

## What you get

| | |
|---|---|
| **Write once, enforce everywhere** | Rules live in this library, not copy-pasted into every repo. Consumers add one dependency and one test class. |
| **Adoption never blocks** | Every rule is frozen. Existing violations are recorded as debt on first run; only *new* ones fail. You can adopt a rule on a codebase that breaks it 200 times, today. |
| **Failures teach** | A violation prints why the rule exists, how to fix it, and how *not* to fake a fix — written for whoever (or whatever) reads the build log. |
| **Runnable at any granularity** | The wiring produces a real JUnit tree, so you can run the whole suite, one group, or a single rule from the IDE gutter. |
| **Composable** | A rule is a class, a group wraps rules, a group can wrap groups. Nests to any depth. |
| **Guarded against silent decay** | The ways this could quietly stop enforcing anything — a rule registered but never evaluated, a group added but never wired — are covered by tests that fail loudly. |

## How it fits together

```mermaid
flowchart LR
    subgraph consumer["Your repo"]
        CT["CentralArchitectureTest<br/><i>@AnalyzeClasses</i>"]
        STORE[("archunit/frozen<br/><i>committed</i>")]
    end

    subgraph lib["llama-guard-sdk"]
        ACR["AllCentralRules"]
        TG["TestingRulesGroup<br/><i>group</i>"]
        R1["TestClassNamingConventionRule"]
        R2["NoMockedRepositoryInIntegrationTestRule"]
        FMT["AgentFriendlyFailureDisplayFormat"]
    end

    CT -->|"ArchTests.in(...)"| ACR
    ACR --> TG
    TG --> R1
    TG --> R2
    R1 & R2 -.->|"violation"| FMT
    FMT -.->|"WHY / HOW TO FIX"| OUT["Build output"]
    R1 & R2 <-->|"known violations"| STORE
```

Each arrow from a group is an `@ArchTest ArchTests` field; each leaf is an `@ArchTest ArchRule`.
Because `ArchTests.in(X)` descends into `X`'s `@ArchTest` fields, the same shape nests indefinitely —
`AllCentralRules` is just a group whose members happen to be groups.

## Quick start

**1. Depend on it** (see [Install](#install) for the GitHub Packages repository and auth):

```xml
<dependency>
  <groupId>io.github.milczekt1</groupId>
  <artifactId>llama-guard-sdk</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

**2. Add one test class** — `src/test/java/com/acme/CentralArchitectureTest.java`:

```java
@AnalyzeClasses(packages = "com.acme", importOptions = ImportOption.DoNotIncludeJars.class)
class CentralArchitectureTest {
    @ArchTest
    static final ArchTests all = ArchTests.in(AllCentralRules.class);
}
```

> Do **not** add `ImportOption.DoNotIncludeTests`. The testing rules inspect your test classes;
> excluding them makes those rules pass vacuously — green build, zero enforcement.

**3. Configure ArchUnit** — `src/test/resources/archunit.properties`:

```properties
freeze.store.default.path=src/test/resources/archunit/frozen
failureDisplayFormat=io.github.milczekt1.llamaguard.format.AgentFriendlyFailureDisplayFormat
```

**4. Seed the freeze store once, and commit it:**

```bash
mvn test -Darchunit.freeze.store.default.allowStoreCreation=true
git add src/test/resources/archunit/frozen && git commit -m "chore: freeze existing violations"
```

Existing violations are now debt. Only new ones fail.

## What a failure looks like

```text
Architecture Violation [test.class-naming-convention] [Priority: MEDIUM]

WHY:
  Most build tools select which top-level classes to run by class-name convention
  (Maven's Surefire and Failsafe plugins, for example, match *Test and *IT
  respectively). A top-level class holding JUnit test methods — @Test,
  @ParameterizedTest, @RepeatedTest, @TestFactory or @TestTemplate — whose name
  ends in neither Test nor IT is silently never executed: it looks like coverage
  in the source tree while proving nothing in CI.

HOW TO FIX:
  Rename the reported top-level class to end in Test (unit tests) or IT
  (integration tests) so your build tool's test-selection convention picks it up…

HOW NOT TO FIX (this rule):
  Do NOT delete the test methods or the class to make this rule pass, and do NOT
  widen your build tool's test-include configuration instead of renaming…

HOW NOT TO FIX (always):
  - Do NOT edit, hand-write, or delete files under archunit/frozen/ to make a NEW
    violation disappear…
  - Do NOT silence the rule with @SuppressWarnings, @ArchIgnore, comments, or by
    disabling the test.
  - …

Offending locations:
  Class <com.acme.InvalidlyNamedTestClass> does not have simple name ending with 'IT'…
```

The anti-fix policy is appended to **every** failure and cannot be dropped per rule — the point is
that the cheap escape routes are named before someone reaches for one.

`failureDisplayFormat` is global per run, but the formatter falls back to ArchUnit's standard output
for any rule it does not own, so your own ArchUnit tests render unchanged.

## Rules

| Rule id | Group | What it enforces |
|---|---|---|
| `test.no-mocked-repository-in-integration-test` | `TestingRulesGroup` | An `*IT` class must not declare a mocked (`@Mock`, `@MockitoBean`, `@MockBean`) field whose type ends in `Repository` or `Dao`. |
| `test.class-naming-convention` | `TestingRulesGroup` | A top-level class holding JUnit test methods (`@Test`, `@ParameterizedTest`, `@RepeatedTest`, `@TestFactory`, `@TestTemplate`) must end in `Test`, `Tests` or `IT`. Nested classes — including JUnit 5 `@Nested` groups — are exempt: they run through their enclosing class. |

This table is maintained by hand; nothing in the build checks it.

> **Rule ids are freeze-store keys.** Changing an id orphans every consumer's frozen entry, so treat
> it as a breaking change.

## How freezing decides

The single thing worth understanding before you trust the build:

```mermaid
flowchart TD
    START["Rule evaluated"] --> KNOWN{"Rule has an entry<br/>in stored.rules?"}
    KNOWN -->|"No — never frozen"| SEED["Record all violations as debt<br/><b>build PASSES</b>"]
    KNOWN -->|"Yes"| DIFF{"Any violation not<br/>already recorded?"}
    DIFF -->|"No"| PASS["<b>build PASSES</b>"]
    DIFF -->|"Yes"| FAIL["Report only the new ones<br/><b>build FAILS</b>"]

    style SEED fill:#fff4ce,stroke:#b8860b
    style FAIL fill:#ffd7d7,stroke:#b22222
    style PASS fill:#d7f5d7,stroke:#2e8b57
```

Two consequences follow, and they are the questions people actually ask:

- **A rule added today, violated in three months, fails.** The first run records an index entry (with
  zero violations). Three months later that violation is new, so the build fails. It is *not*
  accepted as debt.
- **Commit the `stored.rules` change** produced by that first run. Uncommitted, CI sees no entry,
  takes the left branch above, and absorbs the first violation silently — a rule that looks armed and
  is not.

> **Changing a rule's predicate invalidates frozen entries for it.** ArchUnit matches known
> violations by their rendered *text*, so widening a rule from `Test`/`IT` to `Test`/`Tests`/`IT`
> rewrites every message it produces — the frozen entries stop matching and the same old violations
> resurface as new ones. That is a breaking change for consumers on the same footing as renaming an
> id: they must re-freeze. Batch predicate changes into a release and say so in the notes.

## Install

The artifact is published to GitHub Packages, which Maven does not know about by default:

```xml
<repositories>
  <repository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/MilczekT1/LLamaGuard</url>
    <snapshots><enabled>true</enabled></snapshots>
  </repository>
</repositories>
```

GitHub Packages requires authentication even for public reads. Add a matching server to
`~/.m2/settings.xml` — the `<id>` must equal the repository `<id>`:

```xml
<servers>
  <server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_GITHUB_TOKEN</password>  <!-- classic PAT with read:packages -->
  </server>
</servers>
```

Without both pieces the build fails with `Could not find artifact io.github.milczekt1:llama-guard-sdk`.
In CI use a secret, not a checked-in token (`${env.GITHUB_TOKEN}` interpolates in `settings.xml`).

`0.1.0-SNAPSHOT` is a snapshot; no release has been cut yet.

## Configuration reference

| Property | Required | Notes |
|---|---|---|
| `freeze.store.default.path` | yes | Resolved against the JVM's **working directory**, not the classpath. Maven sets it to the module directory; an IDE run configuration with a different working directory will not find the store. |
| `failureDisplayFormat` | recommended | Without it you get ArchUnit's default one-line output instead of WHY / HOW TO FIX. |
| `freeze.store` | optional | Set to `io.github.milczekt1.llamaguard.store.EmptyOmittingViolationStore` to keep empty violation files out of your commits — see below. |
| `freeze.store.default.allowStoreCreation` | **never commit as `true`** | See the warning below. |

> **Do not put `freeze.store.default.allowStoreCreation=true` in `archunit.properties`.** ArchUnit
> defaults it to `false`, and that default is the only thing separating "the store is missing" from
> "silently freeze everything and pass". Pinned to `true`, a store that was never committed, got
> gitignored, or was lost to a shallow checkout gives you a green build with every violation
> re-frozen and no signal at all. Left at its default, the same situation fails loudly with
> `Creating new violation store is disabled (…)`. ArchUnit merges any `archunit.`-prefixed system
> property, which is why the one-off override in step 4 works without editing the committed file.

### The freeze store

`freeze.store=io.github.milczekt1.llamaguard.store.EmptyOmittingViolationStore` changes two things
about how the store is written.

**Violation files are named after the rule id.** Stock ArchUnit names them with a random UUID, so
reading a store means resolving names through `stored.rules` first:

```text
archunit/frozen/
├── stored.rules
├── test.class-naming-convention        # instead of 56d55a4e-91ac-4e12-8682-030d6f3f746f
└── acme.no-stdout-in-services
```

`git log -p archunit/frozen/test.class-naming-convention` is then that rule's debt history. Only ids
get this treatment: the store is global, so it also serves rules frozen without a `RuleDoc`, whose
descriptions are whole sentences — those keep a UUID. Names are assigned once, when a rule is first
frozen, so existing stores keep their UUIDs and keep working.

**A clean rule leaves no file.** A rule that is already clean still gets frozen: ArchUnit records it
in `stored.rules` *and* writes an empty violation file. The index entry is what keeps the rule
enforced (see the flowchart above); the empty file is noise in a commit. This store keeps the entry
and drops the file. Three things to know:

- **Opt-in.** Leave the line out and you get ArchUnit's stock store, empty files included.
- **Global per run.** ArchUnit uses it for *every* `FreezingArchRule` in the run, including your own.
  The behaviour change is narrow and the index entry is always preserved, so your own frozen rules
  stay exactly as enforced.
- **Not retroactive.** Switching it on leaves already-committed empty files in place —
  `FreezingArchRule` only writes when it has something to change, and a rule that is clean and stays
  clean never triggers a write. Clear them once with `mvn test -Darchunit.freeze.refreeze=true` (or
  delete them by hand) and review the diff: `refreeze` re-records *every* rule, so anything currently
  failing gets absorbed as debt too.

## Extending

### The shape

One rule, one class. A rule class under `rules/<topic>/` owns everything about that rule; a group
under `groups/` is a thin wrapper composing rule classes; `AllCentralRules` is a group of groups.

Packages split by role, and the arrows only point one way:

| package | holds | depends on |
|---|---|---|
| root | `DocumentedRule` — the authoring contract | `doc` |
| `doc` | `RuleDoc`, `RuleRegistry` — the vocabulary | nothing |
| `store` | `EmptyOmittingViolationStore` | `doc` |
| `format` | `AgentFriendlyFailureDisplayFormat`, `AntiFixPolicy` | `doc` |
| `rules/<topic>` | the rules themselves | root, `doc` |
| `groups` | composition only | `rules/<topic>` |

`store` and `format` are peers: a doc is rendered on failure whether or not freezing did anything
with it, so neither imports the other.

Membership is declared once, as `@ArchTest ArchTests` fields. `ArchTests.in(X)` descends into
exactly those fields and nothing else, so the field *is* the membership — there is no second list to
keep in step, and no way to declare a member that consumers never evaluate.

### Adding a rule to an existing group

1. Create `rules/<topic>/<RuleName>Rule.java` — a `final class implements DocumentedRule` with a
   private constructor (see `TestClassNamingConventionRule`). **Class names end in `Rule`**, so a
   rule class is recognisable at a glance and never collides with the `*Test` convention its own
   tests follow.
   - `static final RuleDoc DOC` — id is `<topic>.<kebab-case-rule>`, matching
     `^[a-z0-9]+(\.[a-z0-9-]+)+$`. Returned from `doc()`.
   - `static final ArchRule RULE` — the raw rule, package-private. Returned from `definition()`.
     Tests exercise *this*; the published field is frozen, so it seeds and passes, which would make
     rule-correctness tests meaningless.
   - `@ArchTest public static final ArchRule rule = new <RuleName>Rule().guard();` — `guard`
     registers the doc, renames the rule to the doc id (that name is the freeze-store key), and
     allows an empty `should`. **Declare it below `DOC` and `RULE`**: it runs during class
     initialisation and reads them. Method order does not matter — only fields initialise.
2. In the group, give it an `@ArchTest ArchTests` field. That field is what consumers evaluate; a
   rule class nobody points at is never run.
3. Add fixtures under `src/test/java/.../fixtures/<topic>/` — at least one class the rule must flag
   and one it must leave alone. Surefire excludes `**/fixtures/**`, so fixtures named `*Test`/`*IT`
   are not executed. Then write `rules/<topic>/<RuleName>RuleTest.java` against the raw `RULE`, asserting
   **both** directions: a test that only asserts what the rule ignores passes vacuously if the scan
   ever finds nothing.
4. Assert in the rule's own test that the published field carries the doc id, as
   `publicRuleIsFrozenAndIdPinned` does. Freezing a rule under another rule's doc is not possible —
   `guard()` reads both off the same object — but nothing yet checks that the `@ArchTest` field
   exists at all, which an interface cannot enforce.
5. Extend the expected id set in `AllCentralRulesTest.ruleDiscoveryDescendsThroughNestedGroups`.
6. Add a row to the [Rules](#rules) table. Nothing enforces this — it is on you.

### A rule in more than one group

Membership is a graph, not a tree: nothing stops two groups naming the same rule class, and over
time a rule genuinely belonging to two axes (say testing *and* security) will.

That works. It also costs something:

| | |
|---|---|
| JUnit nodes | **one per path** — the rule appears once under each group |
| Rule evaluation | **once per node** — predicates re-run over the same classes |
| Class import | once — ArchUnit caches `JavaClasses` per `@AnalyzeClasses` |
| Freeze store | one entry — both nodes share the description, so they cannot disagree |

Nothing deduplicates the nodes: ArchUnit builds one per `@ArchTest` field it reaches, and those
fields are static. Collapsing them would mean owning node creation, which is not worth it for a
repeated predicate pass over already-imported classes.

`everyRuleIdIsClaimedByExactlyOneRule` allows this on purpose. It groups published rules by id and
requires **one distinct rule object** per id — a rule reached by two paths is one object read from
one `static final` field, while two rules colliding on an id are two. That collision is the real
hazard, because the id is the freeze-store key, so the two would read each other's recorded
violations as their own. `RuleRegistry` does not catch it: its guard compares docs, so two rules
carrying identical documentation pass straight through.

Default to one group per rule and compose by nesting groups. Reach for a second parent when the
rule really does belong under both.

### Adding a group

`Java17Rules`, `JakartaMigrationRules` and `SpringRules` do not exist yet. On top of the rule steps:

1. Create `groups/<Topic>Rules.java` — copy `TestingRulesGroup`: a `@UtilityClass` with one
   `@ArchTest ArchTests` field per member.
2. Give it an `@ArchTest ArchTests` field on `AllCentralRules`. Without one the group exists but no
   consumer ever evaluates it.

## Contributing

Uses [Lombok](https://projectlombok.org/). Install your IDE's Lombok plugin, or the IDE reports errors
on generated members that `mvn verify` compiles cleanly.

Lombok is wired through `annotationProcessorPaths` in the root `pom.xml`, not just as a dependency —
JDK 23+ ignores annotation processors that are only on the classpath. Remove that block and
compilation fails on the generated members (`RuleDoc.builder()`, the formatter's `log`), so the
misconfiguration cannot pass silently.

Build: `./mvnw verify`. The reactor is `llama-guard-sdk` (the library) and `llama-guard-example` (a working consumer
with a committed freeze store, which doubles as an end-to-end test of the wiring).

## License

MIT
