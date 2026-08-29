# Retiring a rule

**An id should no longer be used — renamed, split, merged, or obsolete.**

Retirement is an **SDK** feature. Any catalog built on `corral-sdk` retires its own ids this way;
nothing here is specific to Corral's rules.

Opting out of someone else's rule locally is a different thing:
[excluding a rule](excluding-a-rule.md).

## Never rename. Retire.

A rule id is the freeze-store key in every consumer's repo. Renaming one fails **silently**: their
recorded violations stay filed under the old id, the rule re-seeds clean, and the build goes green
enforcing nothing.

Not theoretical — renaming one id in this repo left `stored.rules` holding *both* the new entry and a
stale orphaned line for the old one. ArchUnit's store never prunes entries it no longer recognises,
and its `ViolationStore` SPI has no rename verb and nowhere to pass a former id. A rename cannot be
made safe against that. Retire instead.

## Do it

```java
@ArchTest
public static final ArchRule oldName = DeprecatedRule.supersededBy(
        "acme.no-stdout-in-services",          // retired
        "acme.logging.no-stdout-in-services",  // replacement
        "moved under a concern segment");
```

Wire it into the group that used to publish the old id. Unwired, it registers a doc but nothing
evaluates it.

The retired id stays registered as an always-passing signpost naming its replacement, so a consumer
who excluded it keeps building and reads where the rule went. It is deliberately **never frozen** —
freezing would claim it is enforced, and it is not.

## What else to update

Whatever pins your published id set. If you have a golden file or a test asserting exact ids, the
retired id is still published — it now appears alongside its replacement.

## In Corral's own catalog

| | |
|---|---|
| `corral-rules/src/test/resources/published-rule-ids.txt` | Add both ids; `PublishedRuleIdsTest` pins this file |
| `AllCentralRulesTest.ruleDiscoveryDescendsThroughNestedGroups` | Asserts the wired root's ids exactly |
| `RuleIdGrammarTest` | **Nothing to do** — it exempts every id in `DeprecatedRule.retiredIds()` automatically, which is why a retired id may keep a shape the grammar would now reject |

## When to just delete instead

Before your first release there are no consumers and no freeze stores, so an id can simply change.
After that, retire.
