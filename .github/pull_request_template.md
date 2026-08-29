## What and why

<!-- One or two sentences. Link the issue if there is one. -->

## Rule changes

<!-- Delete this whole section if you did not touch a rule. -->

- [ ] No rule **id** was renamed or removed — ids are freeze-store keys, so changing one silently
      unarms the rule in every consumer's build
- [ ] No rule **predicate text** changed — predicate text is also a freeze-store matching key;
      changing it resurfaces old violations on upgrade
- [ ] New or changed rules are tested against the raw `DEFINITION`, not the published frozen field
- [ ] Fixtures cover **both** directions: a class the rule must flag, and one it must leave alone
- [ ] The [rules catalog](../docs/rules.md) matches `RuleRegistry` (maintained by hand)

If you did break either of the first two, describe the migration for existing consumers here:

## Checks

- [ ] `./mvnw clean verify` passes locally
- [ ] The Java baseline is unchanged (17) — raising it is breaking for consumers

<!-- See CONTRIBUTING.md#what-breaks-consumers for why these specific things are called out. -->
