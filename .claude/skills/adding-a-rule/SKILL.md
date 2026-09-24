---
name: adding-a-rule
description: Use when adding a new ArchUnit rule to Corral's central catalog (corral-rules) — creating a rule class, choosing its id, registering it in a group, or a rule test that seems to pass without checking anything
---

# Adding a Rule

## Overview

Adding a rule to Corral's catalog touches 7+ files and has three failure modes that produce a
**green build with zero enforcement** — no test catches them structurally; you have to build in the
right order. This skill is that order, the three traps named explicitly, and a checklist for
proving the rule checks something before you call it done.

Read [docs/creating-a-rule.md](../../../docs/creating-a-rule.md), which is the contract every rule
follows, and [CONTRIBUTING.md § Rule ids](../../../CONTRIBUTING.md#rule-ids) first — this skill
sequences and cross-references them for *this* catalog, it does not restate them.

## The steps, in order

1. **Choose the id.** Grammar, closed vocabulary and caps are in
   [CONTRIBUTING.md § Rule ids](../../../CONTRIBUTING.md#rule-ids). Segment 1 is always the fixed
   `corral.` vendor prefix — never a judgment call. The one judgment call: the segment-2 concern (or,
   for a library-specific rule, the segment-3 qualifier after it) is the non-JDK **library whose
   correct use the rule asserts**, not a library the predicate merely uses to *detect* something. A
   rule that flags JUnit/Mockito types leaking into production code names them only to find them — it
   polices layering, not either library's correct use — so it stays `corral.layering.*`, not
   `corral.test.*` or `corral.test.mockito.*`. Ask "what is this rule actually asserting about?", not
   "what classes does the predicate mention?".

2. **Create the rule class** at
   `corral-rules/src/main/java/io/github/milczekt1/corral/rules/<topic>/<rule>/<Name>Rule.java` — a
   `final class implements DocumentedRule` with a private constructor and a name ending in `Rule`
   (see `TestClassNamingConventionRule`). **Field order is `DOC`, then `DEFINITION`, then the
   `@ArchTest` field** — see Trap 1 below for why this order is load-bearing, not stylistic.

   **If the rule targets another library's type, name it as a fully-qualified string, never a class
   literal**, and add no dependency for it. A class literal is resolved when the field initialises,
   and `guard()` runs at class init — so on a consumer without the library the rule class fails to
   load and takes the whole group node with it. Match as weakly as the defect allows: a package
   prefix through `dependOnClassesThat()` needs no resolution at all, while assignability and
   meta-annotation checks silently return false when the type is absent. See
   [Targeting a library you don't depend on](../../../docs/creating-a-rule.md#targeting-a-library-you-dont-depend-on).

3. **Write the rule test** at `.../rules/<topic>/<rule>/<Name>RuleTest.java`, covering **both
   directions** against the raw `DEFINITION` field — see Trap 2 below for why the published field
   cannot carry these assertions. The test shares the rule's package deliberately: `DEFINITION` is
   package-private, so a test anywhere else cannot reach it without widening the published surface.

4. **Put the examples where nothing will run them and nothing will lint them**, and reach them with
   `importClasses(...)`. Only the compiler has to see them, which is what puts them in test output
   and therefore in `TestScope.TEST_CLASSES`.

   Examples made of plain code — a call, a field, a name — go in `static` nested classes in that
   test, as `NoThreadSleepRule`'s `ThreadSleeper` does: no runner selects them, and what each is
   for is unmissable. Give their methods real bodies.

   Examples carrying test-shaped annotations — `@Test`, `@Disabled`, JUnit 4's `@Before` — go in a
   `fixtures/` package beside the test instead, as `NoJUnit4Rule` and `NoDisabledWithoutReasonRule`
   do. Sonar reads a nested class in a test file as a test of that file and flags the deliberate
   violation under test; `**/fixtures/**` is excluded from Surefire, Failsafe and Sonar, and
   nothing excludes a nested class from analysis. Getting this wrong costs a dozen findings on the
   PR, not a build failure, so it surfaces late.

   **Both directions is two things, not necessarily two classes.** Where the verdict is about a
   *call*, one example carries both — `NoThreadSleepRule`'s `ThreadSleeper` sleeps *and* calls
   `Thread.currentThread`, and the store recording one line rather than two is the assertion. Add a
   second, deliberately-ignored class only when one cannot hold both: a rule whose verdict is about
   the *class* needs one, because the first example is already a violation. What is not optional is
   that an over-broad predicate fails — a lone example with a lone matching call cannot do that,
   since every too-wide predicate still finds exactly that one call.

   Never name a top-level example `*IT` outside a `fixtures` package. Failsafe includes
   `**/*IT.java` and excludes only `**/fixtures/**`, so such a class runs as a real integration
   test — an example that sleeps then really sleeps.

   Give them names where none is a substring of another: the assertions match on the report text.

5. **Freeze the examples into the committed store**, in the same test. Steps 3 and 4 prove the
   predicate; this proves the finding reaches
   `corral-rules/src/test/resources/archunit/frozen/<id>` — the file every consumer's recorded debt
   is the shape of. Committing it makes the store a reviewed artefact: widen or reword a predicate
   and the extra findings fail the build instead of quietly joining the accepted set.

   ```java
   ArchConfiguration.get().setProperty("freeze.store.default.path", "src/test/resources/archunit/frozen");
   try {
       ArchRule frozen = assertInstanceOf(FreezingArchRule.class, TheRule.rule, "...")
               .persistIn(new EmptyOmittingViolationStore());
       frozen.check(EXAMPLES);
       // then read stored.rules and the <id> file back off disk and assert on them
   } finally {
       ArchConfiguration.get().reset();
   }
   ```

   **Hand the store over with `persistIn`; do not name it in `freeze.store`.**
   `FreezingArchRule` captures its store when the rule object is *constructed*, and the published
   field is constructed during class initialisation — at whichever test touches the rule first. A
   `freeze.store` set in a test body therefore races class loading: it passes when the test runs
   alone and silently gets ArchUnit's stock store in a full module run. Only the store *path* goes
   on the process-wide `ArchConfiguration`, reset in a `finally`.

   Seed the store once, then **commit it**:

   ```bash
   ./mvnw test -pl corral-rules -Darchunit.freeze.store.default.allowStoreCreation=true
   ```

   Nothing in the build sets `allowStoreCreation`, so a missing store fails loudly rather than
   re-seeding itself green.

6. **Register it in a group** as an `@ArchTest ArchTests` field (see `TestingRulesGroup`). A rule
   class nobody points at is imported and compiled but never evaluated by any consumer.

   A group is adopted whole, so everything in one is on by default for whoever wires it. A rule a
   team can reasonably decline ships in **no** group instead: list it on `EveryOptInRule` (test
   sources) so the grammar, doc and id-uniqueness tests still walk it, give it a row in the
   **Opt-in rules** table of `docs/rules.md` rather than the main one, and leave
   `PublishedCatalogTest` alone — step 7 does not apply. Consumers wire it with
   `ArchTests.in(TheRule.class)`.

7. **Extend the expected id set** in
   `PublishedCatalogTest.ruleDiscoveryDescendsThroughNestedGroups` (`corral-rules/src/test/java/io/github/milczekt1/corral/groups/PublishedCatalogTest.java`).

8. **Add a row to the [rules catalog](../../../docs/rules.md).** `RulesCatalogDocTest` fails the
   build if you skip it: it checks the id set against the published groups *and* the group named in
   column 2. Only those two columns — a dangling rule id in the prose column passes.

## Three silent failure modes

Each of these leaves the build green while enforcing nothing. None is caught by a structural check;
catching them is why the steps above are ordered the way they are.

| Trap | Why it's silent |
|---|---|
| **`@ArchTest` field declared above `DOC`/`DEFINITION`** | `guard()` runs during static class initialisation, at the moment the `@ArchTest` field's initialiser executes. Static fields initialise in **declaration order**, so a field declared before `DOC`/`DEFINITION` reads them as `null` — method order is irrelevant, only field order matters. The class still compiles; the rule freezes against `null`. |
| **Predicate tested against the published field, not `DEFINITION`** | The published field is wrapped in `FreezingArchRule`. Against a store with no entry for the rule, it seeds every violation as accepted debt and reports zero — the test passes on the very run that should have shown a failure, and every run after. Assert the predicate against the raw `DEFINITION`, which is unfrozen. Step 5 does check the published field, and is *not* this trap: its store is committed, so the run compares against recorded lines instead of seeding. |
| **Consumer sets `ImportOption.DoNotIncludeTests`** | Any rule scoped to test classes (see `TestScope`) then has no test classes to evaluate, so it passes vacuously for that consumer. This one binds *consumers*, not catalog authors — call it out in a rule's Javadoc whenever the rule inspects test-scope code, the way `TestClassNamingConventionRule` does. |

## Three loud failures — and what they mean

Skip a step above and one of these tests fails the build, on purpose:

- **`PublishedCatalogTest`** — fails if the published id set no longer matches its expected set
  (step 6 skipped, or an id changed). If it fails on a rule you did **not** mean to touch, an id got
  renamed somewhere in your diff. **Ids are never renamed** — an id is the freeze-store key, and
  renaming orphans every consumer's recorded violations (rule re-seeds clean, build stays green,
  nothing is enforced). Retire the old id with `DeprecatedRule.supersededBy(retiredId,
  replacementId, why)` in `corral-sdk` instead, and keep the retired id in the published set.
- **`RuleIdGrammarTest`** — fails if the id violates the closed grammar (namespace, polarity marker,
  segment cap) from step 1. Fix the id, not the test.
- **`RulesCatalogDocTest`** — fails if `docs/rules.md` and the published groups disagree, on either
  the id set or the group column (step 8 skipped, or a rule moved group).

## Before you call it done

The steps above wire the rule up; these prove it checks something. Rationale in
[docs/creating-a-rule.md § 6](../../../docs/creating-a-rule.md). Every one of these caught a real
defect in the last rule added here — none was caught by the build.

- [ ] **Each predicate clause mutation-tested.** Delete a clause, run the rule's test, confirm a
      *named* test fails; restore it. `NoThreadSleepRule` reached review with its scope clause and
      its name clause both unpinned behind a green suite. Mutate a *list* entry by entry, not the
      list as a whole — one clause covers for another, so deleting a whole clause can fail a test
      while a single entry of it stays unpinned.
      **Back up `src/test/resources/archunit/frozen/` first and restore it after each run.** The
      published field is frozen, so a mutation that finds fewer violations makes
      `FreezingArchRule` prune the committed store — and `EmptyOmittingViolationStore` deletes the
      file outright when nothing is left. Reseed and re-run before you call the rule done.
- [ ] **The flagged example holds a call the rule must NOT match.** With one matching call and
      nothing else, an over-broad predicate — up to `alwaysTrue()` — finds exactly the recorded
      violation and passes.
- [ ] **No example's name is a substring of another's.** Assertions match on report text, so a
      contained name makes one case pass on another's violations, and a mutation that should have
      killed it survives.
- [ ] **Run whole: `./mvnw test -pl corral-rules`, not `-Dtest=<OneTest>`.** `ArchConfiguration` is
      process-wide and Surefire reuses the JVM; the freeze-store wiring passed alone and failed in a
      full run, twice, for two different reasons.
- [ ] **Test names re-read against their assertions.** Rename any that claim more.
- [ ] **`RuleDoc` re-read against the predicate.** It renders into the failure output, so a dodge it
      warns about but the predicate misses is a claim someone will act on.
- [ ] **`./mvnw clean verify` green**, with the committed store either untouched or reseeded and
      committed on purpose.

## Quick reference

| Artifact | Path |
|---|---|
| Rule class | `corral-rules/src/main/java/io/github/milczekt1/corral/rules/<topic>/<rule>/<Name>Rule.java` |
| Rule test | `corral-rules/src/test/java/io/github/milczekt1/corral/rules/<topic>/<rule>/<Name>RuleTest.java` |
| Examples | nested in that test when they are plain code; `.../<topic>/<rule>/fixtures/` when they carry test annotations |
| Committed freeze store | `corral-rules/src/test/resources/archunit/frozen/<id>` (plus its `stored.rules` line) |
| Group wiring | `corral-rules/src/main/java/io/github/milczekt1/corral/groups/<Topic>RulesGroup.java` |
| Discovery test | `corral-rules/src/test/java/io/github/milczekt1/corral/groups/PublishedCatalogTest.java` |
| Rules table | `docs/rules.md` |

`NoThreadSleepRule` is the worked example. The three rules that predate this layout still sit flat
under `rules/<topic>/`, with their examples in a shared `fixtures/<topic>/` package and no committed
store at all — copy the per-rule shape, not theirs.
