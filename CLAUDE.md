# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Corral publishes ArchUnit rules as a versioned Maven dependency instead of a copy-pasted test class.
Three modules, arrows pointing one way (enforced by the module boundary, not convention):

- **`corral-sdk`** — the framework for authoring rules: `DocumentedRule`, `RuleDoc`/`RuleRegistry`,
  the failure formatter, exclusions, `TestScope`. Published so consumers can build their own catalog
  without adopting Corral's rules. It has no dependency on `corral-rules`.
- **`corral-rules`** — the catalog: concrete rules under `rules/<topic>/`, composed by `@UtilityClass`
  groups under `groups/`.
- **`corral-example`** — a working consumer with a committed freeze store, deliberate violations, a
  consumer-authored rule (`acme.no-stdout-in-services`) and an exclusion. It is the end-to-end test
  of the wiring; breaking it breaks the build. `sonar.skip=true` and coverage thresholds zeroed,
  because its violations are intentional.

## Build & test

```bash
./mvnw clean verify              # full build; JaCoCo check runs at verify and can fail it
./mvnw test -pl corral-rules     # one module
./mvnw test -pl corral-rules -Dtest=NoThreadSleepRuleTest   # one test — see caveat below
```

Java 17 is the baseline (`maven.compiler.release` in the root POM). A newer local JDK is fine.
Lombok is wired via `annotationProcessorPaths`; install the IDE plugin or the IDE reports errors on
generated members that Maven compiles cleanly.

**Run the whole module, not one test class, before believing a result.** ArchUnit's
`ArchConfiguration` is process-wide and Surefire reuses the JVM, so freeze-store and configuration
behaviour that passes in isolation can differ in a full run. Isolation is the misleading case.

`**/fixtures/**` is excluded from Surefire (SDK and rules modules) and Failsafe; Failsafe includes
`**/*IT.java`, so a top-level `*IT` example outside `fixtures` runs as a real test.

Coverage gates (root POM): line ≥ 0.75, branch ≥ 0.70, missed classes = 0, measured on the merged
unit+integration exec file.

## The core invariant: a rule id is a freeze-store key

Everything unusual here follows from this. An id becomes a file name in every consumer's committed
`archunit/frozen/` store, so:

- **Ids are never renamed or removed** — a rename orphans consumers' recorded violations, the rule
  re-seeds clean, the build stays green, and nothing is enforced. Withdraw with
  `DeprecatedRule.supersededBy(...)` (`docs/retiring-a-rule.md`) and keep the old id published.
- **A rule's predicate text is also a matching key.** Rewording it resurfaces old violations in
  consumers' builds on upgrade, on code they did not touch.
- The full grammar (vendor prefix `corral.`, closed segment-2 vocabulary, `no-` / `-must-` polarity
  marker, ≤ 4 segments, ≤ 72 chars) lives in `CONTRIBUTING.md § Rule ids` and is enforced by
  `RuleIdGrammarTest`, which *is* the vocabulary — add a word there and nowhere else.
- Shape-only checks (regex, length, depth) live in `RuleDoc`'s constructor because they bind consumer
  ids too. Vocabulary checks deliberately do **not**, so consumers keep their own namespace.

Other consumer-visible surfaces with no automated check: raising the Java baseline, and letting
`docs/rules.md` drift from the published groups. See `CONTRIBUTING.md § What breaks consumers`.

## How a rule is put together

```java
public final class NoStdoutRule implements DocumentedRule {
    static final RuleDoc DOC = RuleDoc.builder().id("corral.logging.no-system-out")...build();
    static final ArchRule DEFINITION = noClasses()...;          // raw, package-private

    @ArchTest
    public static final ArchRule rule = new NoStdoutRule().guard();   // MUST be last
}
```

`guard()` (default method on `DocumentedRule`) registers the doc, renames the rule to the id, freezes
it, applies exclusions, and interposes `IgnorePatternsGuard`.

**Field order is load-bearing.** `guard()` runs during static init and reads `DOC`/`DEFINITION`;
static fields initialise in declaration order, so an `@ArchTest` field declared first reads `null`.
Nothing enforces this.

Membership is declared once, as `@ArchTest ArchTests` fields on a group. `ArchTests.in(X)` descends
into exactly those fields, so the field *is* the membership — no second list. Groups nest to any
depth; the catalog publishes no root, because composing groups is the consumer's call.
`EveryPublishedGroup` (test sources) is that root for Corral's own tests.

A rule may sit in two groups: it then runs once per path and the catalog test requires one distinct
rule *object* per id (two objects sharing an id is the real hazard — `RuleRegistry` misses it when
their docs are identical).

## Four ways to ship a rule that enforces nothing (all green)

1. `@ArchTest` field declared above `DOC`/`DEFINITION` → freezes against `null`.
2. Testing the predicate through the published (frozen) field instead of raw `DEFINITION` → against a
   store with no entry it seeds every violation as debt and reports zero, forever. Freezing the
   published field against a *committed* store is a different thing and is correct (step 4 below).
3. A consumer setting `ImportOption.DoNotIncludeTests` → every test-scope rule passes vacuously. Call
   this out in the Javadoc of any rule scoped through `TestScope`.
4. A typo in a fully-qualified string naming another library's type → indistinguishable from a
   consumer who does not use that library: the predicate matches nothing. Only a fixture built from
   the real type tells them apart, which is what the test-scope dependency is for.

A green rule test is not evidence. Mutation-test each predicate clause (delete it, confirm a *named*
test fails), and make sure the flagged example also contains a call the rule must **not** match —
otherwise `alwaysTrue()` would pass too.

## Adding a rule

Use the **`adding-a-rule` skill** (`.claude/skills/adding-a-rule/SKILL.md`) — it sequences the 7+
files and the traps. The contract is `docs/creating-a-rule.md`. In short:

| Artifact | Path |
|---|---|
| Rule class | `corral-rules/src/main/java/io/github/milczekt1/corral/rules/<topic>/<rule>/<Name>Rule.java` |
| Test | same package under `src/test/java/...` (`DEFINITION` is package-private) |
| Examples | nested in the test when plain code; a `fixtures/` sub-package when they carry test annotations — Sonar lints nested ones as real tests |
| Committed freeze store | `corral-rules/src/test/resources/archunit/frozen/<id>` + its `stored.rules` line |
| Group wiring | `.../groups/<Topic>RulesGroup.java`, plus a field on `EveryPublishedGroup` |
| Expected id set | `PublishedCatalogTest.ruleDiscoveryDescendsThroughNestedGroups` |
| Catalog table | `docs/rules.md` (hand-maintained, unchecked) |

Seed a store once, then commit it — nothing in the build sets the flag, so a missing store fails loudly:

```bash
./mvnw test -pl corral-rules -Darchunit.freeze.store.default.allowStoreCreation=true
```

Hand the store to a test with `persistIn(new EmptyOmittingViolationStore())`, never by setting
`freeze.store` in a test body: `FreezingArchRule` captures its store at *construction*, which happens
during class init at whichever test touches the rule first. Only the store *path* goes on the
process-wide `ArchConfiguration`, reset in a `finally`.

`NoThreadSleepRule` is the worked example for plain-code examples (nested, committed store) and
`NoJUnit4Rule` for annotation-shaped ones (per-rule `fixtures/`, committed store). The three older
rules sit flat under `rules/<topic>/` sharing one `fixtures/<topic>/` package with no committed
store — copy the newer shape.

## Failure output

`AgentFriendlyFailureDisplayFormat` (activated by the consumer's `archunit.properties`) prints WHY /
HOW TO FIX / HOW NOT TO FIX from `RuleDoc`, plus `AntiFixPolicy`'s ten fixed clauses, appended to
every failure and droppable by no rule. Two are enforced in code: an `archunit_ignore_patterns.txt`
anywhere on the classpath fails every rule (`IgnorePatternsGuard`), and an exclusion in
`corral-exclusions.txt` requires a written reason that is reprinted on every other rule's failure.

`RuleDoc` text is acted on as fact by whoever fixes the violation, increasingly an agent. When the
anti-fix text names a dodge the predicate does not actually catch, say so explicitly.

## Conventions

Conventional Commits, capitalised subject, imperative mood (`feat: Add SpringRules with ...`). Every
change lands on `main` via a squash-merged PR, so the PR title becomes the commit message. `main`
requires linear history and resolved conversations.

Rule classes end in `Rule` so they never collide with the `*Test` convention their tests follow.

## Docs map

`docs/rules.md` (catalog) · `docs/configuration.md` (freezing, store, formatter) ·
`docs/creating-a-rule.md` · `docs/retiring-a-rule.md` · `docs/excluding-a-rule.md` ·
`docs/freezing.md` · `docs/architecture.md` · `docs/release-process.md` ·
`CONTRIBUTING.md` (rule-id grammar, what breaks consumers) ·
`docs/superpowers/` (design notes: OSS readiness review, exemption design, 100 rule proposals).
