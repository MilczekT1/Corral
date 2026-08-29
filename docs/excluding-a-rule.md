# Excluding a rule

## What it is

Permanently removes **one rule** from your build — a rule from any catalog you consume, Corral's or
your company's. The rule still exists and stays enforced for everyone else, and you keep wiring the
catalog root, so new rules still arrive on upgrade. You removed a rule, not the mechanism that
delivers them.

Exclusion is an **SDK** feature with nothing to wire: every rule that goes through
`DocumentedRule.guard()` reads the same file, so it works identically for a catalog you built
yourself.

Withdrawing an id for *everyone* is a different thing: [retiring a rule](retiring-a-rule.md).

## When to use it

| | |
|---|---|
| ✅ | The rule's premise is **false here** — it assumes something about your stack that isn't true, and no per-violation carve-out helps |
| ✅ | The thing it forbids **is your design** — a deliberate, documented decision the whole codebase depends on |
| ❌ | You break it today and mean to fix it later — freezing already handles that |
| ❌ | One violation is legitimate — exclusion is whole-rule only |

**The test:** would you still exclude it in a brand-new, empty repo? If not, it isn't an exclusion.

## How to exclude

One line in `src/test/resources/corral-exclusions.txt`, beside `archunit.properties`:

```text
# <rule-id> :: <reason>
corral.logging.no-system-err :: We ship a CLI; stderr is the interface. ADR-021.
```

That rule now passes without evaluating; everything else is untouched. No file, no change.

| | |
|---|---|
| A reason is **mandatory** | The SDK can't judge if it's a good one, but it can make its absence fatal |
| An id that matches no rule in the run **logs a warning** | A typo, or a rule renamed or retired upstream, removes nothing while reading as though it did |
| Whole rules only, never one violation | Switching a rule off should be loud in review |

Every exclusion in effect is printed under `EXCLUDED IN THIS BUILD` on any rule failure, so a
switched-off rule can't hide.

## ⚠️ Not a pause button

An excluded rule **records nothing** while it's off, so violations acquired meanwhile are all *new*
the day you delete the line — and the build fails on code nobody touched.

**To adopt a rule you currently break, just let it run.** Freezing records your existing violations
as debt; only new ones fail.

## Why the typo check is a warning, not a failure

Telling a typo from a rule simply absent from a partial run needs the complete rule set, which only a
full run has. A failure would have to be certain, so it would need a wired root to walk — and then it
would go silent for anyone wiring a hand-picked set of groups, which is the setup where mistakes are
likeliest.

So it warns instead, from the first rule evaluated, with no root and no wiring. Read it as "matched
no rule *in this run*": always accurate, actionable on a full run.
