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

## 3. Fixtures and test

Write at least one class the rule must **flag** and one it must **ignore**, and test against the raw
`DEFINITION`, asserting **both** directions.

If you keep fixtures in your test sources, exclude them from your test runner — Corral excludes
`**/fixtures/**` in Surefire — so fixtures named `*Test`/`*IT` don't run themselves.

## 4. Wire it into a group

A group is a `@UtilityClass` holding `@ArchTest ArchTests` fields. `ArchTests.in(X)` descends into
exactly those fields, so **the field is the membership** — a rule class nobody points at is never
run.

## ⚠️ Three ways to ship a rule that enforces nothing

| Mistake | Why it's silent |
|---|---|
| `@ArchTest` declared **above** `DOC`/`DEFINITION` | `guard()` runs at class init and reads them — they're still `null`. Only *fields* initialise in order; method order is irrelevant |
| Testing the **published** field instead of `DEFINITION` | The published field is frozen: it seeds on first run and passes. The test proves nothing |
| Consumers setting `ImportOption.DoNotIncludeTests` | Every test-scope rule passes vacuously |

Each gives you a green build and zero enforcement.

## In Corral's own catalog

Contributing a rule *here* adds three project-specific steps:

| | |
|---|---|
| The id follows Corral's grammar | `corral.<concern>.<slug>` — see [Rule ids](../CONTRIBUTING.md#rule-ids). Pinned by `RuleIdGrammarTest` |
| Extend `AllCentralRulesTest.ruleDiscoveryDescendsThroughNestedGroups` | It asserts the wired root's ids exactly, so an id change shows as a diff in review |
| Add a row to the [rules catalog](rules.md) | Nothing enforces this |

And the bar is higher: broadly applicable, objectively checkable, stable — see
[Is a rule catalog-worthy?](../CONTRIBUTING.md#is-a-rule-catalog-worthy). A rule encoding one team's
preference belongs in that team's own catalog, which is what this SDK is for.
