# Excluding a rule

**A rule is wrong for your codebase — not "not yet", but never.**

Withdrawing an id for everyone instead? See [retiring a rule](retiring-a-rule.md).

## Do it

One line in `src/test/resources/corral-exclusions.txt`, beside `archunit.properties`:

```text
# <rule-id> :: <reason>
corral.logging.no-system-err :: We ship a CLI; stderr is the interface. ADR-021.
```

That rule now passes without evaluating; everything else is untouched. You keep
`ArchTests.in(AllCentralRules.class)`, so new rules still arrive on upgrade. No file, no change.

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
