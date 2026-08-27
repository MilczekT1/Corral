# Excluding a rule

**You are a consumer. A rule is wrong for your codebase — not "not yet", but never.**

Not what you want? See [retiring a rule](retiring-a-rule.md) (maintainers, withdrawing an id) or
[creating a rule](creating-a-rule.md).

## Do it

Create `src/test/resources/corral-exclusions.txt`, beside `archunit.properties`:

```text
# <rule-id> :: <reason>
corral.logging.no-system-err :: We ship a CLI; stderr is the interface. ADR-021.
```

That rule now passes without evaluating. Everything else is untouched, and you keep
`ArchTests.in(AllCentralRules.class)` — so new rules still arrive on upgrade.

No file, no change. Nothing to configure.

## Rules of the file

| | |
|---|---|
| One line per rule | `<rule-id> :: <reason>` |
| The reason is **mandatory** | Corral can't judge if it's a good one, but it can make its absence fatal |
| The id must be one Corral publishes | A typo removes nothing while reading as though it did — so it fails the build instead |
| Whole rules only | Never a single violation. Disabling a rule should be loud in review |
| One copy on the classpath | Two copies fail, naming both — otherwise a test-scoped dependency could decide your rules |
| Unreadable file → build fails | A file that isn't understood must not be trusted to remove anything |

Every exclusion in effect is printed under `EXCLUDED IN THIS BUILD` on **any** rule failure, so a
switched-off rule can't hide.

## ⚠️ Not a pause button

An excluded rule **records nothing** while it's off. Violations your codebase acquires meanwhile are
all *new* the day you delete the line — and the build fails on code nobody touched.

**To adopt a rule you currently break, don't exclude it.** Just let it run: freezing records your
existing violations as debt, and only *new* ones fail.

Adding a line here in the same change that made a rule fail is silencing, not excluding — and reads
that way in the diff.

## Your own rules

This file only removes **Corral's** rules. To drop a rule you wrote, delete its `@ArchTest` field
from your group.
