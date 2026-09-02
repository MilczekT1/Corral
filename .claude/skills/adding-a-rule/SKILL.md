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

3. **Write the rule test** at `.../rules/<topic>/<rule>/<Name>RuleTest.java`, covering **both
   directions** against the raw `DEFINITION` field — see Trap 2 below for why the published field
   cannot carry these assertions. The test shares the rule's package deliberately: `DEFINITION` is
   package-private, so a test anywhere else cannot reach it without widening the published surface.

4. **Declare the examples as `static` nested classes in that test**, and reach them with
   `importClasses(...)`. What each is for is then unmissable, and no runner selects them, so they
   need neither an `*IT` name nor a `fixtures` package: only the compiler has to see them, which is
   what puts them in test output and therefore in `TestScope.TEST_CLASSES`.

   **Both directions is two things, not necessarily two classes.** Where the verdict is about a
   *call*, one example carries both — `NoThreadSleepRule`'s `ThreadSleeper` sleeps *and* calls
   `Thread.currentThread`, and the store recording one line rather than two is the assertion. Add a
   second, deliberately-ignored class only when one cannot hold both: a rule whose verdict is about
   the *class* needs one, because the first example is already a violation. What is not optional is
   that an over-broad predicate fails — a lone example with a lone matching call cannot do that,
   since every too-wide predicate still finds exactly that one call.

   Do **not** promote them to top-level classes named `*IT` outside a `fixtures` package. Failsafe
   includes `**/*IT.java` and excludes only `**/fixtures/**`, so such a class runs as a real
   integration test — an example that sleeps then really sleeps. Nested classes sidestep the
   question entirely.

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

7. **Extend the expected id set** in
   `PublishedCatalogTest.ruleDiscoveryDescendsThroughNestedGroups` (`corral-rules/src/test/java/io/github/milczekt1/corral/groups/PublishedCatalogTest.java`).

8. **Add a row to the [rules catalog](../../../docs/rules.md).** Nothing in the build checks
   this table — it drifts silently if you skip it.

## Three silent failure modes

Each of these leaves the build green while enforcing nothing. None is caught by a structural check;
catching them is why the steps above are ordered the way they are.

| Trap | Why it's silent |
|---|---|
| **`@ArchTest` field declared above `DOC`/`DEFINITION`** | `guard()` runs during static class initialisation, at the moment the `@ArchTest` field's initialiser executes. Static fields initialise in **declaration order**, so a field declared before `DOC`/`DEFINITION` reads them as `null` — method order is irrelevant, only field order matters. The class still compiles; the rule freezes against `null`. |
| **Predicate tested against the published field, not `DEFINITION`** | The published field is wrapped in `FreezingArchRule`. Against a store with no entry for the rule, it seeds every violation as accepted debt and reports zero — the test passes on the very run that should have shown a failure, and every run after. Assert the predicate against the raw `DEFINITION`, which is unfrozen. Step 5 does check the published field, and is *not* this trap: its store is committed, so the run compares against recorded lines instead of seeding. |
| **Consumer sets `ImportOption.DoNotIncludeTests`** | Any rule scoped to test classes (see `TestScope`) then has no test classes to evaluate, so it passes vacuously for that consumer. This one binds *consumers*, not catalog authors — call it out in a rule's Javadoc whenever the rule inspects test-scope code, the way `TestClassNamingConventionRule` does. |

## Two loud failures — and what they mean

Skip a step above and one of these two tests fails the build, on purpose:

- **`PublishedCatalogTest`** — fails if the published id set no longer matches its expected set
  (step 6 skipped, or an id changed). If it fails on a rule you did **not** mean to touch, an id got
  renamed somewhere in your diff. **Ids are never renamed** — an id is the freeze-store key, and
  renaming orphans every consumer's recorded violations (rule re-seeds clean, build stays green,
  nothing is enforced). Retire the old id with `DeprecatedRule.supersededBy(retiredId,
  replacementId, why)` in `corral-sdk` instead, and keep the retired id in the published set.
- **`RuleIdGrammarTest`** — fails if the id violates the closed grammar (namespace, polarity marker,
  segment cap) from step 1. Fix the id, not the test.

## Before you call it done

The steps above wire the rule up; these prove it checks something. Rationale in
[docs/creating-a-rule.md § 6](../../../docs/creating-a-rule.md). Every one of these caught a real
defect in the last rule added here — none was caught by the build.

- [ ] **Each predicate clause mutation-tested.** Delete a clause, run the rule's test, confirm a
      *named* test fails; restore it. `NoThreadSleepRule` reached review with its scope clause and
      its name clause both unpinned behind a green suite.
- [ ] **The flagged example holds a call the rule must NOT match.** With one matching call and
      nothing else, an over-broad predicate — up to `alwaysTrue()` — finds exactly the recorded
      violation and passes.
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
| Rule test + examples | `corral-rules/src/test/java/io/github/milczekt1/corral/rules/<topic>/<rule>/<Name>RuleTest.java` |
| Committed freeze store | `corral-rules/src/test/resources/archunit/frozen/<id>` (plus its `stored.rules` line) |
| Group wiring | `corral-rules/src/main/java/io/github/milczekt1/corral/groups/<Topic>RulesGroup.java` |
| Discovery test | `corral-rules/src/test/java/io/github/milczekt1/corral/groups/PublishedCatalogTest.java` |
| Rules table | `docs/rules.md` |

`NoThreadSleepRule` is the worked example. The three rules that predate this layout still sit flat
under `rules/<topic>/`, with their examples in a shared `fixtures/<topic>/` package and no committed
store at all — copy the per-rule shape, not theirs.
