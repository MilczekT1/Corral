---
name: adding-a-rule
description: Use when adding a new ArchUnit rule to Corral's central catalog (corral-rules) — creating a rule class, choosing its id, registering it in a group, or a rule test that seems to pass without checking anything
---

# Adding a Rule

## Overview

Adding a rule to Corral's catalog touches 6+ files and has three failure modes that produce a
**green build with zero enforcement** — no test catches them structurally; you have to build in the
right order. This skill is that order, plus the three traps named explicitly.

Read [CONTRIBUTING.md § Adding a rule to an existing group](../../../CONTRIBUTING.md#adding-a-rule-to-an-existing-group)
and [§ Rule ids](../../../CONTRIBUTING.md#rule-ids) first — this skill sequences and cross-references
them, it does not restate them.

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

3. **Write the rule test** at `.../rules/<topic>/<rule>/<Name>RuleTest.java`, asserting **both
   directions** (flags the bad example, stays silent on the good one) against the raw `DEFINITION`
   field — see Trap 2 below for why the published field cannot substitute. The test shares the
   rule's package deliberately: `DEFINITION` is package-private, so a test anywhere else cannot
   reach it without widening the published surface. Group the assertions with `@Nested`.

4. **Declare the example classes as `static` nested classes at the bottom of that test**, and reach
   them with `importClasses(...)` — at least one class the rule must flag and one it must leave
   alone. What each is for is then unmissable, and no runner selects them, so they need neither an
   `*IT` name nor a `fixtures` package: only the compiler has to see them, which is what puts them
   in test output and therefore in `TestScope.TEST_CLASSES`.

   Do **not** promote them to top-level classes named `*IT` outside a `fixtures` package. Failsafe
   includes `**/*IT.java` and excludes only `**/fixtures/**`, so such a class runs as a real
   integration test — an example that sleeps then really sleeps. Nested classes sidestep the
   question entirely.

   Give them names where none is a substring of another: the assertions match on the report text.

5. **Register it in a group** as an `@ArchTest ArchTests` field (see `TestingRulesGroup`). A rule
   class nobody points at is imported and compiled but never evaluated by any consumer.

6. **Extend the expected id set** in
   `PublishedCatalogTest.ruleDiscoveryDescendsThroughNestedGroups` (`corral-rules/src/test/java/io/github/milczekt1/corral/groups/PublishedCatalogTest.java`).

7. **Add a row to the [rules catalog](../../../docs/rules.md).** Nothing in the build checks
   this table — it drifts silently if you skip it.

## Three silent failure modes

Each of these leaves the build green while enforcing nothing. None is caught by a structural check;
catching them is why the steps above are ordered the way they are.

| Trap | Why it's silent |
|---|---|
| **`@ArchTest` field declared above `DOC`/`DEFINITION`** | `guard()` runs during static class initialisation, at the moment the `@ArchTest` field's initialiser executes. Static fields initialise in **declaration order**, so a field declared before `DOC`/`DEFINITION` reads them as `null` — method order is irrelevant, only field order matters. The class still compiles; the rule freezes against `null`. |
| **Test written against the published field, not `DEFINITION`** | The published field is wrapped in `FreezingArchRule`. On the test's first run it has no freeze-store entry, so it seeds every violation as accepted debt and reports zero — the test passes on the very run that should have shown a failure, and every run after. It proves nothing about the rule's predicate, ever. Test the raw `DEFINITION` field instead, which is unfrozen. |
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

## Quick reference

| Artifact | Path |
|---|---|
| Rule class | `corral-rules/src/main/java/io/github/milczekt1/corral/rules/<topic>/<rule>/<Name>Rule.java` |
| Rule test + examples | `corral-rules/src/test/java/io/github/milczekt1/corral/rules/<topic>/<rule>/<Name>RuleTest.java` |
| Group wiring | `corral-rules/src/main/java/io/github/milczekt1/corral/groups/<Topic>RulesGroup.java` |
| Discovery test | `corral-rules/src/test/java/io/github/milczekt1/corral/groups/PublishedCatalogTest.java` |
| Rules table | `docs/rules.md` |

`NoThreadSleepRule` is the worked example. The three rules that predate this layout still sit flat
under `rules/<topic>/`, with their examples in a shared `fixtures/<topic>/` package — copy the
per-rule shape, not theirs.
