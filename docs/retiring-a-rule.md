# Retiring a rule

**You are a maintainer. An id should no longer be used — renamed, split, merged, or obsolete.**

Not what you want? See [excluding a rule](excluding-a-rule.md) (consumers, opting out locally) or
[creating a rule](creating-a-rule.md).

## Never rename. Retire.

A rule id is the freeze-store key in every consumer's repo. Renaming one fails **silently**: their
recorded violations stay filed under the old id, the rule re-seeds clean, and the build goes green
enforcing nothing.

This isn't theoretical — renaming one id here left `stored.rules` holding *both* the new entry and a
stale orphaned line for the old one. ArchUnit's store never prunes entries it no longer recognises,
and its `ViolationStore` SPI has no rename verb and nowhere to pass a former id. So a rename can't be
made safe. Retire instead.

## Do it

```java
@ArchTest
public static final ArchRule oldName = DeprecatedRule.supersededBy(
        "corral.test.class-naming-convention",           // retired
        "corral.test.class-names-must-end-with-test-or-it", // replacement
        "renamed to carry a polarity marker");
```

The retired id stays registered as an always-passing signpost naming its replacement. A consumer who
excluded it keeps building. It is deliberately **never frozen** — freezing would claim it's enforced.

## Three files

| File | Why |
|---|---|
| The **group** that published the old id | Wire the `@ArchTest` field. Unwired, nothing evaluates it — and `RuleExclusions` validates against the wired root, not the registry |
| `corral-rules/src/test/resources/published-rule-ids.txt` | Add both ids. `PublishedRuleIdsTest` pins this file |
| `AllCentralRulesTest.ruleDiscoveryDescendsThroughNestedGroups` | Asserts the wired root's ids exactly |

**Nothing to do in `RuleIdGrammarTest`** — it exempts every id in `DeprecatedRule.retiredIds()`
automatically. That's why a retired id may keep a shape the grammar would now reject: it predates the
grammar, `corral.` prefix included.

## When to just delete instead

Before `0.1.0` there are no consumers and no freeze stores, so an id can simply change. After the
first release, retire.
