# Excluding a rule

## What it is

Permanently removes **one Corral rule** from your build. The rule still exists and stays enforced for
everyone else — you keep `ArchTests.in(AllCentralRules.class)`, so new rules still arrive on upgrade.
You removed a rule, not the mechanism that delivers them.

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
| A reason is **mandatory** | Corral can't judge if it's a good one, but it can make its absence fatal |
| The id must be one Corral publishes | A typo removes nothing while reading as though it did, so it fails the build |
| Whole rules only, never one violation | Switching a rule off should be loud in review |

Every exclusion in effect is printed under `EXCLUDED IN THIS BUILD` on any rule failure, so a
switched-off rule can't hide.

## ⚠️ Not a pause button

An excluded rule **records nothing** while it's off, so violations acquired meanwhile are all *new*
the day you delete the line — and the build fails on code nobody touched.

**To adopt a rule you currently break, just let it run.** Freezing records your existing violations
as debt; only new ones fail.

---

Only Corral's rules go here. To drop a rule you wrote, delete its `@ArchTest` field from your group.
