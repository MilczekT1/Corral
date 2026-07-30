# Design: Central ArchUnit Rules Framework

**Date:** 2026-07-13
**Status:** Approved design — ready for implementation planning

---

## Problem

Architecture rules that are **not project-specific** (banned annotations, migration
guards, test hygiene) are currently copy-pasted between repositories, or worse,
enforced only by prose in `CLAUDE.md`. There is no single, deterministic mechanism
to write a rule once and enforce it across many Java modules.

Three prior artifacts each solved part of this, inconsistently:

- **tournaments** (`2026-06-08-archunit-no-repo-mock-in-it-design.md`, Gradle) — rules
  written once in `:shared` testFixtures, consumed per-module via a thin
  `CentralArchitectureTest` using `@AnalyzeClasses` + `ArchTests.in(...)`, with rich
  `WHY`/`HOW` failure messages. **No freezing** — adoption blocks on existing violations.
- **mekagojira** (`2026-03-20-cross-module-archunit-design.md`, Gradle) — rule *groups* +
  `FreezingArchRule` + a real per-repo `archunit.properties` freeze store. Closest to the
  target, but glued to that repo and using an interface/`@Test default`/`implements`
  consumption model.
- **Ingested requirements message** — a standalone **Maven** framework
  (`central-arch-rules-framework/`) with `RuleRegistry`, group classes,
  `AgentFriendlyFailureDisplayFormat`, freezing, and an example consumer.

This design **merges all three** into one consistent framework.

## Goal

- A standalone, reusable Maven library of centralized, non-project-specific ArchUnit rules.
- Consumed from any Java module via a single test-scoped dependency + one thin test class.
- Rules organized into **groups**, runnable as the whole suite, by group, or individually.
- Every rule wrapped in `FreezingArchRule` so adoption never blocks in-flight work.
- Failure messages are **agent- and human-readable**: they explain *why* a rule exists,
  *how to fix* a violation, and — critically — **how NOT to "fix" it** (no cheating the rule).
- Enforcement is deterministic (a failing test), **not** a script that mutates `CLAUDE.md`.

## Non-negotiable principles

1. **Tests enforce, not `CLAUDE.md`.** No script mutates `CLAUDE.md` across repositories as
   the enforcement mechanism. `CLAUDE.md` may *guide*; the failing test is the gate.
2. **Freeze, don't block.** Existing violations are seeded into a per-consumer store on first
   run; only *new* violations fail the build.
3. **Failures teach and forbid cheating.** Every failure prints `WHY` / `HOW TO FIX` from the
   rule's `RuleDoc`, plus an **extendable global anti-fix policy** (see § Anti-fix policy).

---

## Decisions locked during brainstorming

| # | Decision | Choice |
|---|----------|--------|
| 1 | Home & distribution | **New standalone Maven repo** `central-arch-rules-framework`, group `com.yourorg.archrules`, published to GitHub Packages, consumed as a test-scoped JAR. |
| 2 | Anti-fix guidance | **Global anti-fix policy** shown on every failure (extendable) **+ structured `RuleDoc`** per rule carrying rule-specific specifics. |
| 3 | Per-rule doc shape | **Structured `RuleDoc`** (`id` / `why` / `howToFix` / optional `howNotToFix`), not freeform `.because()`. Keeps rich text out of the freeze-store key. |
| 4 | Consumer wiring | **`@ArchTest` fields + `ArchTests.in(...)`** (native ArchUnit JUnit5 rules-library pattern). Enables running the whole suite, a group, or a single rule. |
| 5 | First-cut rule scope | **Database + Testing groups, fully fleshed**; Java17 / JakartaMigration / Spring deferred (documented as "add here", not created). |
| 6 | Freeze store | **Per-consumer store, committed** under `src/test/resources/archunit/frozen/`. Framework JAR ships no stored violations. |
| 7 | Spec location | This spec lives in the Chassis repo for now; the framework repo is scaffolded during implementation. |

---

## Architecture

A standalone Maven library, `central-arch-rules-framework` (group `com.yourorg.archrules`),
published to GitHub Packages and consumed as a **test-scoped JAR**. It fuses:

- **Structure** from tournaments — write once, group, run per-module via a thin test class.
- **Freezing + rich failure text** from mekagojira — every rule is a `FreezingArchRule`.
- **Framework shape** from the ingested message — `RuleRegistry`, group classes,
  `AgentFriendlyFailureDisplayFormat`, example consumer.

### Repository layout

```text
central-arch-rules-framework/
  pom.xml                      # standalone; publishes to GitHub Packages
  README.md                    # rules table generated/verified from RuleRegistry
  src/main/java/com/yourorg/archrules/
    RuleDoc.java               # structured: id / why / howToFix / howNotToFix
    RuleRegistry.java          # enumerates every rule + its RuleDoc (queryable)
    FrozenRules.java           # helper: freeze(ArchRule, RuleDoc) -> wrapped rule
    format/
      AgentFriendlyFailureDisplayFormat.java   # renders WHY / FIX / DO-NOT + global policy
      AntiFixPolicy.java                        # baseline global policy, extendable
    groups/
      DatabaseRules.java       # SEEDED (fully fleshed)
      TestingRules.java        # SEEDED (fully fleshed)
      AllCentralRules.java     # aggregates all seeded groups
      # Java17Rules / JakartaMigrationRules / SpringRules — deferred, NOT created in first cut
  src/test/java/...            # framework's own tests (see § Testing the framework)
  examples/llama-rules-example/
    pom.xml
    src/test/java/.../CentralArchitectureTest.java
    src/test/resources/archunit.properties
    src/test/resources/archunit/frozen/.gitkeep
```

`FrozenRules.java` and `format/AntiFixPolicy.java` are additions beyond the ingested
message's file list — they keep freezing and the extendable policy DRY.
`Java17Rules` / `JakartaMigrationRules` / `SpringRules` are named for orientation and
documented in the README as the growth path, but **not created** in the first cut.

---

## Core model: rule + `RuleDoc` + registry

Each rule is a `public static final ArchRule` field inside a group class, already wrapped
by `FrozenRules.freeze(...)`. The `RuleDoc` is the single source of agent-facing text.
The **short, stable `id`** is what ArchUnit uses as the freeze-store key; the **rich prose**
is attached via the failure format — deliberately keeping rich text *out* of the store key
(the fragility mekagojira hit, where reworded multi-line `.because()` strings silently
re-seeded the store).

```java
public final class DatabaseRules {

    static final RuleDoc NO_SPRING_TX = RuleDoc.builder()
        .id("db.no-spring-transactional")
        .why("CockroachDB needs retryable transactions; Spring's @Transactional cannot provide them.")
        .howToFix("Inject a Transactor and wrap the work: transactor.inTransaction(() -> { ... }) "
                + "or transactor.supplyInTransaction(() -> value).")
        .howNotToFix("Do NOT swap in @Transactional(propagation=REQUIRES_NEW) or any other flavor — "
                   + "all variants are banned.")
        .build();

    @ArchTest
    public static final ArchRule noSpringTransactional = FrozenRules.freeze(
        noClasses().that().resideInAPackage("..")
            .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional"),
        NO_SPRING_TX);   // short stable key = NO_SPRING_TX.id(); rich text via formatter
}
```

- **`RuleDoc`** — immutable value type: `id`, `why`, `howToFix`, optional `howNotToFix`.
  `id` is the stable freeze key and must be unique across all rules.
- **`RuleRegistry`** — lists every rule's `RuleDoc` so (a) a framework test asserts every
  rule has non-blank `why`/`howToFix` and a unique `id`, and (b) the README rules table is
  generated/verified from it — docs cannot drift from code.
- **`FrozenRules.freeze(rule, doc)`** — the linchpin. It (a) sets the rule description to the
  stable `doc.id()` via `.as(doc.id())`, (b) wraps the result in `FreezingArchRule.freeze(...)`,
  and (c) registers `doc` in `RuleRegistry` keyed by `doc.id()`. Because ArchUnit derives the
  freeze-store key **from the rule description**, pinning the description to `doc.id()` gives a
  short, stable store key — *and* the same `id` is what the formatter uses to look the `RuleDoc`
  back up. One key serves both purposes; the rich prose lives only in the `RuleDoc`, never in
  the description/store key.

### How the rich text reaches the failure (resolved)

Verified against ArchUnit docs (`/tng/archunit`):

- ArchUnit exposes a **`FailureDisplayFormat` SPI**: implement
  `com.tngtech.archunit.lang.FailureDisplayFormat.formatFailure(HasDescription rule,
  FailureMessages messages, Priority priority)` and register it in `archunit.properties` via
  `failureDisplayFormat=<fqn>`. `AgentFriendlyFailureDisplayFormat` implements this SPI.
- On a violation, the formatter reads `rule.getDescription()` (which equals `doc.id()`), looks
  up the `RuleDoc` in `RuleRegistry`, and renders `WHY` / `HOW TO FIX` / per-rule `howNotToFix`
  / the global anti-fix policy, then appends ArchUnit's standard per-violation lines.
- **`failureDisplayFormat` is a global per-run setting** — it applies to *every* `ArchRule` in
  the consumer's run, including the consumer's own rules. Therefore the formatter **must** fall
  back to ArchUnit's standard rendering for any rule whose description is not a known `RuleDoc`
  id. A consumer's unrelated ArchUnit tests are unaffected.

---

## Seeded groups (first cut)

### `DatabaseRules` (precedent: mekagojira `@Transactional` ban)

- `noSpringTransactionalOnClasses` — no class annotated
  `org.springframework.transaction.annotation.Transactional`.
- `noSpringTransactionalOnMethods` — no method annotated with it either (banned in both positions).
- `noRawJdbcOutsideRepositories` — classes using `java.sql` / `javax.sql` / `JdbcTemplate`
  must reside in a `..repository..` / `..jdbc..` package.
  *(Proves "multiple rules per group"; exact allowed-package predicate refined during implementation.)*

### `TestingRules` (precedent: tournaments CAS-6446)

- `integrationTestsMustNotMockRepositoriesOrDaos` — a class whose simple name ends
  `IntegrationTest` / `IT` must not declare a field that is **both** annotated with a
  forbidden mock annotation **and** whose raw type's simple name ends in `Repository` / `Dao`.
  Forbidden annotations matched **by FQN string** (framework needs none on its classpath):
  - `org.mockito.Mock`
  - `org.springframework.test.context.bean.override.mockito.MockitoBean`
  - `org.springframework.boot.test.mock.mockito.MockBean` (deprecated; future-proofing)
- `testClassNamingConvention` — test classes follow the `*Test` / `*IT` naming so
  surefire/failsafe pick them up correctly.
  *(Exact predicate refined during implementation.)*

### Cross-cutting rule conventions

- Every rule wrapped via `FrozenRules.freeze(...)` with its own `RuleDoc`.
- Every rule uses `.allowEmptyShould(true)` so a module with zero matching classes stays
  green rather than failing vacuously.
- `AllCentralRules` re-exposes both groups via `ArchTests.in(DatabaseRules.class)` /
  `.in(TestingRules.class)` so a consumer can opt into everything with one field.

---

## Consumer wiring

One thin class per consuming module — **no rule logic**:

```java
@AnalyzeClasses(packages = "com.acme", importOptions = ImportOption.DoNotIncludeJars.class)
class CentralArchitectureTest {
    @ArchTest static final ArchTests database = ArchTests.in(DatabaseRules.class);
    @ArchTest static final ArchTests testing  = ArchTests.in(TestingRules.class);
    // or, for everything:  @ArchTest static final ArchTests all = ArchTests.in(AllCentralRules.class);
}
```

This yields three run-granularities as a real JUnit test tree:

- **Whole central test** → run the class.
- **A group** → run the `database` node (from IDE gutter or `-Dtest=` method selection).
- **One rule** → run its leaf node.

Critical wiring notes (carried from the source specs):

- **Must NOT** use `ImportOption.DoNotIncludeTests` — `TestingRules` inspects the test
  classes themselves; excluding them makes those rules pass **vacuously**. `DoNotIncludeJars`
  is kept so dependency jars are not scanned.
- The **archunit-junit5 engine** must be on the consumer's test runtime classpath. The
  framework JAR declares `archunit-junit5` as a **compile/api** dependency so consumers
  inherit it transitively via the single test-scoped dependency.
- Consumers set `packages` to their own root package; the framework hard-codes nothing
  project-specific.

---

## Freezing & the extendable global anti-fix policy

### Freeze store — per consumer, committed (framework ships none)

```properties
# consumer: src/test/resources/archunit.properties
freeze.store.default.allowStoreCreation=true
freeze.store.default.path=src/test/resources/archunit/frozen

# register the agent-friendly formatter so WHY / HOW TO FIX / anti-fix policy render on failure
failureDisplayFormat=com.yourorg.archrules.format.AgentFriendlyFailureDisplayFormat
```

The `failureDisplayFormat` line is what activates the rich, anti-fix-carrying messages.
Because it is a global per-run setting, `AgentFriendlyFailureDisplayFormat` falls back to
ArchUnit's default rendering for any rule that is not a framework rule (see § Core model),
so the consumer's own ArchUnit tests are unaffected.

First run seeds current violations into `src/test/resources/archunit/frozen/`; the consumer
commits it; only *new* violations fail thereafter. Adoption never blocks in-flight work.

### Anti-fix policy

A baseline `AntiFixPolicy` is rendered by `AgentFriendlyFailureDisplayFormat` at the bottom
of **every** failure and is **extendable** — consumers/rule authors may *append* clauses;
they cannot silently replace the baseline.

Baseline policy text:

> **HOW NOT TO FIX THIS — these are violations of process, not fixes:**
> - Do **not** edit, hand-write, or delete files under `archunit/frozen/` to make a *new*
>   violation disappear. The store records pre-existing debt only; new violations must be
>   fixed in code.
> - Do **not** silence a rule with `@SuppressWarnings`, `@ArchIgnore`, comments, or by
>   disabling the test.
> - Do **not** rename a class/field/package solely to dodge a name-based rule (e.g. renaming
>   `FooIT` so the IT rule stops matching).
> - Do **not** narrow `@AnalyzeClasses(packages=...)` or add `ImportOption`s to hide code
>   from the scan.
> - Do **not** downgrade, remove, or reword the rule to weaken it.
> - **The only acceptable resolution is changing the production/test code so the rule
>   genuinely passes** — then follow this rule's `HOW TO FIX`.

The per-rule `howNotToFix` (from `RuleDoc`) prints just above this baseline, for
rule-specific traps (e.g. "don't swap `@Transactional` for a different propagation").
Because the policy renders on every failure from the framework's own formatter, it can never
be forgotten per-rule.

### Failure message shape

```
Architecture Violation [db.no-spring-transactional]

WHY:
  CockroachDB needs retryable transactions; Spring's @Transactional cannot provide them.

HOW TO FIX:
  Inject a Transactor and wrap the work: transactor.inTransaction(() -> { ... })
  or transactor.supplyInTransaction(() -> value).

HOW NOT TO FIX (this rule):
  Do NOT swap in @Transactional(propagation=REQUIRES_NEW) or any other flavor — all variants are banned.

HOW NOT TO FIX (always):
  - Do not edit/delete archunit/frozen/ to hide a new violation.
  - Do not @SuppressWarnings / @ArchIgnore / disable the test.
  - Do not rename to dodge a name-based rule.
  - Do not narrow @AnalyzeClasses scope or add ImportOptions to hide code.
  - Do not weaken/remove the rule.
  - Only fix: change the code so the rule genuinely passes.

Offending locations:
  <ArchUnit's standard per-violation lines>
```

---

## Testing the framework

The framework is a library; **its own tests** verify the plumbing (TDD applies here):

- **`RuleRegistry` completeness** — every registered rule has non-blank `id` / `why` /
  `howToFix`; every `id` is unique and stable (guards the freeze key).
- **Formatter** — given a synthetic violation, the rendered message contains `WHY`,
  `HOW TO FIX`, the per-rule `howNotToFix`, and the full global anti-fix policy.
- **Freezing behavior** — a fixture package with a known violation: first run seeds + passes;
  a *new* violation fails; the seeded one stays green (using a temp store dir).
- **Rule correctness** — each seeded rule checked against a small `@AnalyzeClasses` fixture
  containing both a compliant and a violating sample class.
- **Example consumer** (`examples/llama-rules-example`) doubles as an integration smoke test: it
  wires `CentralArchitectureTest`, commits a frozen store, and its build stays green.

### Error handling / graceful degradation

- `allowEmptyShould(true)` everywhere — no vacuous failures.
- Annotations/types matched by FQN string, so a missing optional dependency never breaks a rule.
- The formatter never throws — a rule with no registered `RuleDoc` falls back to a generic
  message rather than failing the run.

---

## Out of scope (first cut)

- Java17 / JakartaMigration / Spring rule groups (named, documented as growth path; not created).
- Any script that mutates `CLAUDE.md` across repos as an enforcement mechanism.
- Detecting inline `Mockito.mock(XRepository.class)` calls in method bodies (ArchUnit cannot
  reliably resolve the mocked type).
- Rolling the framework out to specific consumer repos (separate follow-up per repo).
- Gradle consumer examples (the framework JAR is build-tool-agnostic, but only a Maven
  consumer example ships in the first cut).

---

## Open items to pin during implementation

- Exact allowed-package predicate for `noRawJdbcOutsideRepositories`.
- Exact predicate for `testClassNamingConvention`.
- Final `RuleDoc` builder API surface (field set is fixed: `id` / `why` / `howToFix` /
  optional `howNotToFix`; only the builder ergonomics are open).
- Whether `RuleRegistry` is populated eagerly at group-class load or lazily on first
  `freeze(...)` call — must be resolved so the formatter never sees an unregistered id for a
  framework rule (fallback covers correctness either way, but eager registration is cleaner).

---

## Verification

1. `./mvnw -pl central-arch-rules-framework verify` — framework unit tests green
   (registry completeness, formatter, freezing behavior, rule correctness).
2. `examples/llama-rules-example` build green — `CentralArchitectureTest` runs, frozen store seeds,
   no new violations.
3. Manual: introduce a deliberate `@Transactional` in the example consumer → the build fails
   with the full `WHY` / `HOW TO FIX` / anti-fix message; remove it → green again.
