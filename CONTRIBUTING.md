# Contributing to Corral

Thanks for considering a contribution. This file covers building the project, the conventions a
change is expected to follow, and — most importantly — [the ways a change to this library can look
green and still be wrong](#what-breaks-consumers).

By participating you agree to the [Code of Conduct](CODE_OF_CONDUCT.md).

## Getting started

Build:

```bash
./mvnw verify
```

The reactor is `corral-sdk` (the framework), `corral-rules` (the rule catalog) and `corral-example`
(a working consumer with a committed freeze store, which doubles as an end-to-end test of the
wiring).

**Java 17.** That is the baseline for the whole project, `maven.compiler.release=17` in the root
POM. It is the minimum JVM that can load the published classes, so raising it is a breaking change
for consumers — please do not, without discussing it first. A newer JDK on your machine is fine:
`javac` cross-compiles against its bundled `ct.sym`, so you do not need a JDK 17 installed to build.

**Lombok.** Install your IDE's [Lombok](https://projectlombok.org/) plugin, or the IDE reports errors
on generated members that `mvn verify` compiles cleanly.

Lombok is wired through `annotationProcessorPaths` in the root `pom.xml`, not just as a dependency —
JDK 23+ ignores annotation processors that are only on the classpath. Remove that block and
compilation fails on the generated members (`RuleDoc.builder()`, the formatter's `log`), so the
misconfiguration cannot pass silently.

**Coverage.** JaCoCo fails the build at `verify` below the thresholds in the root `pom.xml`
(`jacoco.lineCoverage.minimum`, `jacoco.branches.minimum`, `jacoco.classes.maxMissed`).
`corral-example` overrides them to zero — it is a wiring demo, not a tested component.

**Sonar.** `corral-example` sets `sonar.skip=true` for the same reason: its classes exist to be
flagged by rules, and its violations are deliberate and committed, so analysing it reports the demo
as findings. It is still compiled and its tests still run — that module is the end-to-end test of
the wiring, and breaking it breaks the build.

## What breaks consumers

Corral's compatibility surface is unusual and sharp. Two of these have **no automated check**, so
they are on the reviewer and on you:

| Change | Why it breaks | Checked by |
|---|---|---|
| Renaming or removing a rule **id** | The id is the freeze-store key. Consumers' recorded violations are filed under the old id, so the rule silently stops enforcing — a green build with zero enforcement. It also silences any consumer excluding that id: the exclusion now names nothing, which logs a warning rather than failing the build. | `AllCentralRulesTest` — the change shows as a diff in its expected id set |
| Changing a rule's **predicate text** | Predicate text is also a freeze-store matching key. On upgrade, old violations resurface and the consumer's build fails on code they did not touch. | nothing |
| Raising the **Java baseline** | It is the minimum JVM that can load the published classes. | nothing |
| Testing against the **published frozen field** instead of the raw `DEFINITION` | The frozen field seeds and passes, so the test is vacuous. | partially |
| Letting the **README rules table** drift from `RuleRegistry` | The most visible form of rot on the landing page. | nothing (yet) |

If you must make one of the first two changes, say so explicitly in the PR and describe the
migration for existing consumers.

## The shape

One rule, one class. A rule class under `rules/<topic>/` owns everything about that rule; a group
under `groups/` is a thin wrapper composing rule classes; `AllCentralRules` is a group of groups.

Packages split by role, and the arrows only point one way:

| module | package | holds | depends on |
|---|---|---|---|
| `corral-sdk` | root | `DocumentedRule` — the authoring contract | `doc` |
| `corral-sdk` | `doc` | `RuleDoc`, `RuleRegistry` — the vocabulary | nothing |
| `corral-sdk` | `store` | `EmptyOmittingViolationStore` | `doc` |
| `corral-sdk` | `format` | `AgentFriendlyFailureDisplayFormat`, `AntiFixPolicy` | `doc`, `exclude` |
| `corral-sdk` | `exclude` | `Exclusion`, `RuleExclusions` — the consumer's `corral-exclusions.txt` | `doc`, `reflect` |
| `corral-sdk` | `reflect` | `PublishedRules` — the `@ArchTest` walk | nothing |
| `corral-sdk` | `scope` | `TestScope` — shared predicates for what a rule applies to | nothing |
| `corral-rules` | `rules/<topic>` | the rules themselves | root, `doc`, `scope` |
| `corral-rules` | `groups` | composition only | `rules/<topic>` |

The module boundary is what enforces the direction: `corral-sdk` has no dependency on
`corral-rules`, so a framework class importing a concrete rule does not compile.

`store` and `format` are peers: a doc is rendered on failure whether or not freezing did anything
with it, so neither imports the other. `format` reads `exclude` in one direction only — to print the
census of what is not being enforced — and `exclude` knows nothing about rendering. `exclude` reads
`reflect` for one thing: walking a wired root is the only way to get a *complete* set of rule ids,
which `RuleRegistry` cannot give mid-run.

Membership is declared once, as `@ArchTest ArchTests` fields. `ArchTests.in(X)` descends into
exactly those fields and nothing else, so the field *is* the membership — there is no second list to
keep in step, and no way to declare a member that consumers never evaluate.

## Adding a rule to an existing group

The step-by-step is **[Creating a rule](docs/creating-a-rule.md)** — id, class, fixtures, wiring, and
the three ways to ship a rule that silently enforces nothing.

What that guide does not cover, because it belongs to the design rather than the task:

- **One rule, one class.** A rule class under `rules/<topic>/` owns everything about that rule; a
  group is a thin wrapper. Class names end in `Rule`, so they never collide with the `*Test`
  convention their own tests follow.
- **Membership is the field.** `ArchTests.in(X)` descends into exactly `X`'s `@ArchTest` fields, so
  the field *is* the declaration. There is no second list to keep in step.

## A rule in more than one group

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

## Adding a group

`Java17Rules`, `JakartaMigrationRules` and `SpringRules` do not exist yet. On top of the rule steps:

1. Create `corral-rules/src/main/java/io/github/milczekt1/corral/groups/<Topic>Rules.java` — copy `TestingRulesGroup`: a `@UtilityClass` with one
   `@ArchTest ArchTests` field per member.
2. Give it an `@ArchTest ArchTests` field on `AllCentralRules`. Without one the group exists but no
   consumer ever evaluates it.

## Is a rule catalog-worthy?

Not every good rule belongs here. A rule earns a place in the central catalog when it is:

- **Broadly applicable** — true for most JVM projects, not a house style.
- **Objectively checkable** — no judgement call about intent.
- **Stable** — the predicate is unlikely to need rewording, because rewording it is a breaking
  change for every consumer.

A rule that encodes one team's preference is better off in that team's own rule namespace. The SDK
exists so you can author those without forking anything.

## Rule ids

An id is the freeze-store key (see [What breaks consumers](#what-breaks-consumers)), so its grammar
is closed and the closure is enforced, not a style convention.

**Every id Corral publishes starts with the `corral.` vendor prefix.** Corral can enforce its own ids
against its own catalog test (`RuleIdGrammarTest`), but it deliberately cannot enforce a consumer's —
`RuleDoc` is public SDK surface, narrowed to universal hygiene precisely so a consumer keeps their own
namespace. That freedom means a consumer authoring rules with `corral-sdk` reaches for the same
generic words Corral's own catalog used to claim — `test`, `logging`, `api`, `security`, `naming`.
Before the prefix, a collision there was prevented only by a runtime `RuleRegistry.register` throw
that forced the *consumer* to rename *their* rule. Taking `corral.` for every id this library
publishes makes the collision structurally impossible instead, and leaves the entire generic
namespace to consumers. A consumer's own rule needs no prefix at all — `corral-example`'s
`acme.no-stdout-in-services` is exactly that case, and it must keep working.

| Shape | When | Example |
|---|---|---|
| `corral.<slug>` | Corral's own framework meta-check | (none published today) |
| `corral.<concern>.<slug>` | a catalog rule | `corral.logging.no-system-out` |
| `corral.<concern>.<library>.<slug>` | a catalog rule specific to one library | `corral.test.mockito.no-static-mocking` |

**Segment 1 is always `corral`** — provenance, not taxonomy. **Segment 2 is a closed vocabulary**, in
four kinds:

- **concern** — `api`, `concurrency`, `exception`, `layering`, `logging`, `naming`, `security`, `test`
- **library** — `jackson`, `jakarta`, `lombok`, `spring`
- **JDK** — `java`, plus `java<N>` for a version-gated API
- **meta** — the one exception: a depth-2 `corral.<slug>` id has no segment-2 concern at all, the slug
  itself sits at segment 2. Nothing published today takes this shape — the framework's own former
  meta-check, `corral.exclusions-must-name-real-rules`, was replaced by an unconditional SDK warning
  that needs no wired root and so is no longer published as a rule at all — but the grammar still
  supports it for a future one. It would still need a polarity marker like any other id — see the
  rule below.

**Segment 3, when present, is a library qualifier** — today, exactly `mockito`, `powermock` or
`junit`.

**Tie-break when a predicate touches a library:** the segment-3 qualifier is the non-JDK library whose
*correct use* the rule asserts, not any library the predicate merely detects. A rule that flagged test
libraries leaking into production code, for instance, would name JUnit and Mockito only to detect
them — it polices layering, not correct use of either library — so it would stay `corral.layering.*`
rather than take on either library's qualifier.

**Exactly two polarity markers.** A slug either starts with `no-` (a prohibition) or contains
`-must-`, read as `<subject>-must-<predicate>` (`fields-must-be-final`: the subject is fields, the
predicate is being final). Six inconsistent forms across the early catalog collapsed into these two
so a slug's intent is legible without opening the rule.

**Caps:** depth ≤ 4 segments, and a fourth segment is legal only when segment 3 is a library
qualifier — today, exactly `mockito`, `powermock` or `junit`. Anything finer-grained belongs in a
group, which can be reorganised, not in the id, which cannot once a consumer has frozen it. Length ≤
72 characters — segment 1 is a fixed vendor prefix on top, so the taxonomy budget below it is
unchanged from before the prefix existed.

**The check is split across two layers, deliberately.** `RuleDoc`'s constructor
(`throwOnInvalidId`) enforces only the shape regex, the length cap and the depth cap — universal
hygiene that binds every id, including a consumer's own, because the id becomes a *file name* in
every consumer's freeze store (see [The freeze store](README.md#the-freeze-store)). The vendor
prefix, the concern vocabulary, the polarity marker and the qualifier-segment list are enforced
separately, by `RuleIdGrammarTest` in `corral-rules`, and only against ids reachable from
`AllCentralRules` — this catalog, not the world. `corral-sdk` is published precisely so a consumer can
author rules in their own namespace ("[a rule that encodes one team's preference is better off in
that team's own rule namespace](#is-a-rule-catalog-worthy)"); a closed vocabulary enforced inside
`RuleDoc` itself would revoke that promise. `corral-example`'s `acme.no-stdout-in-services` is exactly
that case, and it must keep working.

**Deprecate, never rename.** A rule id is a freeze-store key, and ArchUnit's `ViolationStore` SPI has
no rename verb — so a rename orphans every consumer's recorded violations, silently. Withdraw an id
with `DeprecatedRule.supersededBy(...)` instead: **[Retiring a rule](docs/retiring-a-rule.md)**.

## Commit conventions

Conventional Commits, capitalised subject, imperative mood:

```
feat: Add SpringRules with spring.no-field-injection
fix: Resolve Sonar findings and add SonarCloud badges
docs: Split the contributing guide out of the README
build: Target Java 17 across the whole project
ci: Bump actions/create-github-app-token from 2.2.2 to 3.2.0
refactor: Rename the project from LLamaGuard to Corral
```

Every change lands on `main` through a squash-merged pull request, so the PR title becomes the
commit message. Write the title accordingly.

## Pull requests

- `./mvnw clean verify` passes locally before you open it.
- Fill in the PR template, including the rule-change checklist if you touched a rule.
- `main` requires linear history, an up-to-date branch, and all conversations resolved.
- Sonar runs on PRs from branches in this repository. On PRs from forks it is skipped, because the
  token is not exposed to forks — that is expected and does not block the merge.

## Reporting problems

Bugs and rule proposals go in [issues](https://github.com/MilczekT1/Corral/issues).

**Security vulnerabilities do not** — see [SECURITY.md](SECURITY.md).
