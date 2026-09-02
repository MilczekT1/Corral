# Creating a rule

Rules are an **SDK** feature. Depend on `corral-sdk` and you can build a catalog for your own
company in exactly the way Corral builds its own — you do not have to adopt Corral's rules, or any
of them.

This guide is the contract. Corral's catalog is just one instance of it; the extra steps that apply
only there are at the bottom.

> Working in Claude Code? The `adding-a-rule` skill walks these steps interactively.

## 1. Choose an id

`<vendor>.<whatever-you-like>` — pick a vendor segment you own (`acme`, `corral`, …) so your ids can
never collide with a catalog you also consume.

The SDK enforces **shape only**: the id pattern, ≤ 72 characters, ≤ 4 dot-separated segments. It
deliberately does not police your vocabulary — that is yours to decide. Corral's own taxonomy is a
test in `corral-rules`, not a rule in the SDK.

The id is the freeze-store key, so treat it as permanent: see [retiring a
rule](retiring-a-rule.md).

## 2. Write the rule class

A `final class implements DocumentedRule` with a private constructor. **Field order is
load-bearing:**

```java
static final RuleDoc DOC = RuleDoc.builder().id("acme.no-stdout-in-services")...build();
static final ArchRule DEFINITION = noClasses()...;      // raw, package-private

@ArchTest
public static final ArchRule rule = new NoStdoutInServicesRule().guard();   // last
```

`guard()` registers the doc, renames the rule to the id, freezes it, and applies exclusions.

## 3. Examples and test

Cover **both** directions against the raw `DEFINITION` — the published field is frozen, so it would
seed and pass (see the traps below). Both directions is two *things*, not necessarily two classes.

Where the verdict is about a **call**, one example carries both: a method that sleeps, plus a call on
the same owner that is not a sleep. Write a second, deliberately-ignored class only when one example
cannot hold both — a rule whose verdict is about the **class** (its name, its fields) needs one,
because the first example is already a violation.

Whatever shape you pick, an over-broad predicate has to fail. A single example with a single matching
call cannot manage that: every predicate that matches too much still finds exactly that one call, and
passes.

Put the test in the **rule's own package**: `DEFINITION` is package-private, and a test anywhere
else cannot reach it without widening what the rule publishes.

The examples must not run as tests themselves. Corral declares them as `static` nested classes at
the top of the rule's own test, which no runner selects — so they need no `fixtures` package and no
naming contortion, and what each one is for is unmissable. If you keep them as top-level classes
instead, exclude them from your test runner (Corral excludes `**/fixtures/**` in both Surefire and
Failsafe), remembering that Failsafe *includes* `**/*IT.java`.

## 4. Freeze the examples into a committed store

Testing the predicate is half of it. The finding also has to reach the freeze store, under the
rule's id, in the format your consumers will read — so freeze the **published** field against a
store you commit, and assert on the files:

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

A committed store turns the rule's output into a reviewed artefact: reword or widen the predicate
and the extra findings **fail** the build rather than quietly joining the accepted set. It is also
why this is not the "tested the frozen field" trap below — that trap is a store with no entry for
the rule, which seeds and passes; a committed store compares.

Two things to know, both learned the hard way:

- **Hand the store over with `persistIn`, do not name it in `freeze.store`.** `FreezingArchRule`
  captures its store when the rule object is *constructed*, and the published field is constructed
  during class initialisation — at whichever test touches the rule first. Setting `freeze.store` in
  a test body races class loading: green alone, stock store in a full run. Only the *path* belongs
  on the process-wide `ArchConfiguration`, reset in a `finally`.
- **Keep the examples at the top of the file.** Stored violations carry the source line of each
  call, so examples below the tests rewrite the store whenever a test above them gains a line.

## 5. Wire it into a group

A group is a `@UtilityClass` holding `@ArchTest ArchTests` fields. `ArchTests.in(X)` descends into
exactly those fields, so **the field is the membership** — a rule class nobody points at is never
run.

## 6. Prove it actually checks something

A green rule test is not evidence. Four checks, roughly in order of how often they catch something:

- **Delete each clause of the predicate and watch a *named* test fail.** Not the suite — a specific
  test. A clause no test reacts to could be anything. This is the highest-yield check by a distance,
  because the flagged example passes either way, so nothing about a green run distinguishes a
  precise predicate from a reckless one.
- **Run the whole suite, not one test class.** ArchUnit's `ArchConfiguration` is process-wide and
  test runners reuse the JVM, so a rule that behaves in isolation can pick up another test's
  configuration in a full run. Isolation is the misleading case, not the honest one.
- **Read every test name back against what it asserts.** A name that claims more than its assertion
  does is worse than no test: it marks the gap as covered.
- **Read `RuleDoc` back against the predicate.** `why`, `howToFix` and `howNotToFix` render into the
  failure output and are acted on as fact — by people, and increasingly by agents. Where the
  anti-fix text names a dodge the predicate does not catch, say so: name where the rule's sight
  ends, and that what lies past it is still wrong but unenforced.

One shaping decision makes all four easier: **one id, one mistake**. An id is a freeze-store key, so
two problems sharing one can never be adopted, frozen or retired separately — and a predicate
covering both is harder to pin clause by clause than two predicates covering one each.

## ⚠️ Three ways to ship a rule that enforces nothing

| Mistake | Why it's silent |
|---|---|
| `@ArchTest` declared **above** `DOC`/`DEFINITION` | `guard()` runs at class init and reads them — they're still `null`. Only *fields* initialise in order; method order is irrelevant |
| Testing the **predicate** through the published field instead of `DEFINITION` | The published field is frozen. Against a store with no entry for the rule it seeds on first run and passes; the test proves nothing. Freezing it against a *committed* store, as in step 4, is a different thing and is fine |
| Consumers setting `ImportOption.DoNotIncludeTests` | Every test-scope rule passes vacuously |

Each gives you a green build and zero enforcement.

## In Corral's own catalog

Contributing a rule *here* adds these project-specific steps:

| | |
|---|---|
| One rule, one package | `rules/<topic>/<rule>/` in main sources, mirrored in test sources by the rule's test — so everything about a rule is one directory name. `NoThreadSleepRule` is the worked example; the three older rules still sit flat under `rules/<topic>/` with a shared `fixtures/<topic>/` package and no committed store, and move as they are next touched |
| The id follows Corral's grammar | `corral.<concern>.<slug>` — see [Rule ids](../CONTRIBUTING.md#rule-ids). Pinned by `RuleIdGrammarTest` |
| Commit the freeze store | `corral-rules/src/test/resources/archunit/frozen/<id>`. Seed it once with `./mvnw test -pl corral-rules -Darchunit.freeze.store.default.allowStoreCreation=true`, then commit. Nothing in the build sets that flag, so a missing store fails loudly |
| Extend `PublishedCatalogTest.ruleDiscoveryDescendsThroughNestedGroups` | It asserts the wired root's ids exactly, so an id change shows as a diff in review |
| Add a row to the [rules catalog](rules.md) | Nothing enforces this |

And the bar is higher: broadly applicable, objectively checkable, stable — see
[Is a rule catalog-worthy?](../CONTRIBUTING.md#is-a-rule-catalog-worthy). A rule encoding one team's
preference belongs in that team's own catalog, which is what this SDK is for.
