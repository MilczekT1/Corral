# Design: Rule-Per-Class Architecture, Empty-Omitting Freeze Store, Docs Untracking

**Date:** 2026-07-30
**Status:** Approved design — ready for implementation planning

---

## Problem

Three unrelated issues, all in the `rules` module or its repo hygiene.

1. **Group classes do not scale.** `TestingRules` is 145 lines for **two** rules — each rule's
   `RuleDoc`, raw predicate and frozen field live inline, alongside shared constant lists. At 50
   testing rules that file is ~3,600 lines. Rules cannot be found, reviewed, or moved
   independently, and a group cannot be composed from smaller groups without also inheriting that
   bulk.
2. **The freeze store commits empty files.** ArchUnit's `TextFileBasedViolationStore` writes one
   violation file per rule even when the rule is clean, so a consumer's committed store fills with
   zero-byte files that carry no information.
3. **Superpowers specs and plans are tracked.** They are working documents, not library artifacts,
   and should not ship in the repository.

## Goals

- One rule per class, self-contained: its documentation, its predicate, its frozen field.
- Groups act as thin wrappers over rules, and compose into other groups to arbitrary depth.
- A clean rule leaves **no** violation file, while remaining frozen so a later violation fails.
- `docs/superpowers/` stops being tracked, without deleting anything from disk.

## Non-goals

- No new rules. This is a restructure of the two that exist.
- No rule id changes. Ids are freeze-store keys; changing one orphans consumers' frozen violations.
- No change to `RuleDoc`, `RuleRegistry`, `FrozenRules`, or the failure formatter.
- No change to `maven.compiler.release` (stays 25) or to the Lombok wiring.

---

## Verified facts (spikes already run — do not re-litigate)

1. **Three-level `ArchTests` nesting works.** Spiked on ArchUnit 1.4.2: an aggregator holding
   `ArchTests.in(group)`, a group holding `ArchTests.in(ruleClass)`, and a rule class holding an
   `@ArchTest ArchRule` all resolve. The JUnit tree renders
   `AllRules > TopicRules > NamingRule > rule`, so every rule stays individually runnable, and the
   rule genuinely evaluated (the spike's deliberate violation was reported).
2. **A custom store is registered by property.** `ViolationStoreFactory` reads
   `freeze.store=<fully-qualified-class-name>` from `archunit.properties` and instantiates it
   reflectively — so the class needs a public no-arg constructor.
3. **`TextFileBasedViolationStore` is `public final`.** It can be delegated to, so the decorator
   does not have to reimplement store-file handling.
4. **`ViolationStore` has exactly four methods:** `initialize(Properties)`, `contains(ArchRule)`,
   `save(ArchRule, List<String>)`, `getViolations(ArchRule)`.
5. **An unknown rule seeds and passes.** `FreezingArchRule.evaluate` runs
   `if (!store.contains(delegate) || refreezeViolations()) return storeViolationsAndReturnSuccess(result);`
   — so if the store does not know a rule, whatever violations exist are recorded as debt and the
   build **passes**. This is why the index entry must survive even when a rule is clean: dropping
   it would make that rule's first real violation get absorbed silently instead of failing.

---

## Part 1 — Rule classes

Each rule becomes one class under `io.github.milczekt1.archrules.rules.<topic>`:

```text
rules/src/main/java/io/github/milczekt1/archrules/
  rules/
    testing/
      NoMockedRepositoryInIntegrationTest.java
      TestClassNamingConvention.java
```

Shape, carried over unchanged from the current proven pattern:

```java
public final class TestClassNamingConvention {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("test.class-naming-convention")
            .why(...)
            .howToFix(...)
            .howNotToFix(...)
            .build();

    static final ArchRule RULE = classes().that()... ;

    @ArchTest
    public static final ArchRule rule = FrozenRules.freeze(RULE, DOC);

    private TestClassNamingConvention() {
    }
}
```

`DOC` and `RULE` stay package-private: unit tests live in the same package and exercise the **raw**
rule, because a frozen rule seeds its violations and passes, which would make a rule-correctness
test unable to detect a broken predicate. Only `rule` is public — it is what consumers evaluate.

**Shared constants move into the rule that uses them.** `TestingRules` currently holds
`FORBIDDEN_MOCK_ANNOTATIONS` and `JUNIT_TEST_ANNOTATIONS` as group-level fields; each is used by
exactly one rule. `FORBIDDEN_MOCK_ANNOTATIONS` moves into `NoMockedRepositoryInIntegrationTest`,
`JUNIT_TEST_ANNOTATIONS` into `TestClassNamingConvention`. This is what makes a rule class
self-contained; if a future constant genuinely serves several rules, it moves to a shared class in
the topic package at that point, not before.

Class names read as the rule they enforce (`TestClassNamingConvention`), not as a description of the
check. The rule id remains the authority — the class may be renamed freely, the id may not.

## Part 2 — Groups

Groups stay in `io.github.milczekt1.archrules.groups`. A group declares each member **twice**: an
entry in a `MEMBERS` list that tooling reads, and an `@ArchTest ArchTests` field that ArchUnit
descends into.

```java
public final class TestingRules {

    private static final List<Class<?>> MEMBERS = List.of(
            NoMockedRepositoryInIntegrationTest.class,
            TestClassNamingConvention.class);

    @ArchTest
    public static final ArchTests noMockedRepositoryInIntegrationTest =
            ArchTests.in(NoMockedRepositoryInIntegrationTest.class);

    @ArchTest
    public static final ArchTests testClassNamingConvention =
            ArchTests.in(TestClassNamingConvention.class);

    public static List<Class<?>> members() {
        return MEMBERS;
    }

    private TestingRules() {
    }
}
```

The duplication is deliberate and was chosen over reflective derivation: it is explicit and
greppable. Its cost is that the two declarations can drift, so **every group carries a guard test**
asserting that `members()` and the set of `@ArchTest ArchTests` fields describe the same classes,
failing in either direction. Without that test a member present in only one place either silently
goes unenforced or silently disappears from tooling.

A member may be a rule class or another group — that is what makes groups composable. `MEMBERS` is
therefore `List<Class<?>>`, not a narrower type.

`AllCentralRules` keeps its present shape, with `groups()` renamed to `members()` for consistency,
and `loadAll()` recursing through `members()` so that nested groups' rule classes are initialised
and their docs reach `RuleRegistry`. Recursion must tolerate a member that is a rule class (a leaf)
as well as one that is a group.

## Part 3 — Empty-omitting freeze store

New class `io.github.milczekt1.archrules.freeze.EmptyOmittingViolationStore`, registered by
consumers in `archunit.properties`:

```properties
freeze.store=io.github.milczekt1.archrules.freeze.EmptyOmittingViolationStore
```

It implements `ViolationStore` by delegating to a `TextFileBasedViolationStore` instance:

| Method | Behaviour |
|---|---|
| `initialize(Properties)` | Delegate, and retain `default.path` for file lookups. |
| `contains(ArchRule)` | Delegate unchanged — the index entry is what keeps a rule frozen. |
| `save(ArchRule, List<String>)` | Delegate, then if the violation list was empty, delete the file the delegate just wrote. The `stored.rules` entry stays. |
| `getViolations(ArchRule)` | If the rule's file is absent, return an empty list; otherwise delegate. |

Net effect: `stored.rules` lists every rule; only rules with real violations have a file on disk. A
clean rule is still recorded as frozen, so its first violation **fails the build** rather than being
absorbed (verified fact #5). Adopting a new rule onto already-violating code still freezes that debt.

**Accepted coupling:** mapping a rule to its filename requires reading `stored.rules`, whose
`<rule-id>=<uuid>` layout belongs to `TextFileBasedViolationStore`. That class is public, but the
layout is not a documented contract. A test pins the assumption so an ArchUnit upgrade that changes
it fails loudly rather than silently corrupting a consumer's store.

The store must have a public no-arg constructor (verified fact #2).

## Part 4 — Untrack the superpowers docs

```bash
git rm -r --cached docs/superpowers
```

plus `docs/superpowers/` in `.gitignore`. Files remain on disk; git history still contains them.

This spec is committed **before** that step, so it exists in history as the record of the decision.

---

## Migration

Small today, which is the point of doing it now rather than at 50 rules:

- `TestingRules` (145 lines) becomes two rule classes plus a ~30-line group.
- `TestingRulesTest` splits into one test class per rule, in the rule's package, exercising `RULE`.
- `TestingRulesFrozenFieldsTest` and `FrozenFieldStores` follow the rules they cover.
- `AllCentralRulesTest` extends to assert the `members()`/field agreement recursively.
- `rules-example`'s committed store is unaffected: rule ids do not change, so entries stay valid.
  The empty file for `test.no-mocked-repository-in-integration-test` disappears once the new store
  is configured.

## Testing

- **Rule correctness** — per-rule test against the raw `RULE`, using the existing fixtures. These
  must keep passing unchanged; a rule that changes behaviour during a move is a defect, not a
  refactor.
- **Group membership guard** — per group, `members()` and `@ArchTest ArchTests` fields agree, failing
  in both directions. Demonstrate it by introducing divergence temporarily.
- **Composability** — a test asserting the nested tree resolves, so the three-level structure cannot
  silently regress.
- **Registry completeness** — every rule reachable from `AllCentralRules.members()` recursively has a
  registered `RuleDoc`; the locked id set stays `test.class-naming-convention` and
  `test.no-mocked-repository-in-integration-test`.
- **Store behaviour** — a clean rule produces an index entry and no file; a violating rule produces
  both; a rule whose file is absent reads back as zero violations; a violation introduced after a
  clean freeze **fails**. That last case is the one that matters and must be tested explicitly.
- **Full reactor** green, plus `-Dsurefire.runOrder=reversealphabetical`, a standing requirement
  since an earlier review found an order-dependent test passing only by filesystem luck.

## Verification

1. `./mvnw -B verify` — reactor green.
2. Rule count unchanged: `AllCentralRules` still exposes exactly two rule ids.
3. `rules-example` store: `stored.rules` has both entries; only the naming rule has a file.
4. Introduce a violation of the previously-clean rule in `rules-example` — build **fails**. Revert.
5. `git ls-files docs/superpowers` — empty.
6. `git status --porcelain` clean apart from the pre-existing untracked
   `central-arch-rules-framework-design.patch`.

## Out of scope

- Adding rules, or reviving `DatabaseRules`.
- Reflective derivation of group membership (explicitly rejected in favour of explicit lists).
- Changing `RuleDoc`, `RuleRegistry`, `FrozenRules`, or `AgentFriendlyFailureDisplayFormat`.
- The three parked robustness findings from the earlier whole-branch review.
- Publishing, CI, sources/javadoc jars.
