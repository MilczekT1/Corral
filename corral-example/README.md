# Example consumer

A working consumer of `corral-rules`: one test-scoped dependency, the catalog's rules wired in, and a
rule this project owns.

**One test package per feature.** Each package below demonstrates exactly one thing, and each wires a
*different* set of rules — a rule id is the freeze-store key, so the same rule run from two nodes
would have both writing the same entry.

| File | What it shows |
|---|---|
| `wiring/ProjectRulesGroup` | **Your own catalog root.** Corral publishes groups, not an all-in-one root, so the set of rules this build runs is a line in this repo. Note that it deliberately does *not* wire everything the catalog offers. |
| `wiring/ProjectArchitectureTest` | Running that root — the whole point of the dependency. |
| `custom/NoStdoutInServicesRule` | Writing **your own** rule with the library's machinery, with its own anti-fix guidance. It is wired from `ProjectRulesGroup`, alongside the library's. |
| `exclusions/ExcludedRuleTest` | A second node with its own scope, wiring `LoggingRulesGroup` — the group holding the excluded rule. |
| `retirement/DateRulesGroup` | **Withdrawing an id** without renaming it: the group publishes the live rule *and* `acme.no-java-util-date`, the id it replaced, as an always-passing signpost. |
| `retirement/NoLegacyDateApiRule`, `retirement/RetirementTest` | The rule the retired id points at, and the node that runs both. |
| `archunit/frozen/` | The committed freeze store. Six entries, three files named after their rule ids — the clean rules have no file, and the retired id has no entry at all. |
| `corral-exclusions.txt` | Removing one rule from this build permanently, while keeping the rest of the catalog. |
| `InvalidlyNamedTestClass`, `service/NoisyService` | Deliberate, permanent violations, frozen as debt. `NoisyService` is debt for two rules at once: this project's `acme.no-stdout-in-services` and the library's `corral.logging.no-system-out`. |
| `exclusions/StderrWriterAllowedByExclusion` | A violation that is **not** debt — `corral.logging.no-system-err` is frozen clean here, so this would fail the build. The exclusion is what keeps it green, and the class is named after that fact. |

## Writing your own rule

Any `ArchRule` works with `@ArchTest`. But a plain one renders through ArchUnit's default format —
one line, no guidance:

```text
Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package '..service..'
should call method PrintStream.println(String)' was violated (1 times): …
```

Implement `DocumentedRule`, as `NoStdoutInServicesRule` does, and it behaves like a built-in rule:
guidance on failure, and existing violations recorded as debt instead of blocking the build.

```java
public final class NoStdoutInServicesRule implements DocumentedRule {

    static final RuleDoc DOC = RuleDoc.builder()
            .id("acme.no-stdout-in-services")
            .why(...)
            .howToFix(...)
            .howNotToFix(...)
            .build();

    static final ArchRule DEFINITION = noClasses()
            .that().resideInAPackage("..service..")
            .should().callMethod(PrintStream.class, "println", String.class);

    @ArchTest
    public static final ArchRule rule = new NoStdoutInServicesRule().guard();

    @Override public ArchRule definition() { return DEFINITION; }

    @Override public RuleDoc doc() { return DOC; }
}
```

Ids are freeze-store keys, so use your own namespace (`acme.`) and treat a rename as a breaking
change.

## Anti-fix guidance

Two layers, and they do not mix:

| | Where it lives | Scope |
|---|---|---|
| `HOW NOT TO FIX (this rule):` | your rule's `RuleDoc.howNotToFix` | that rule only |
| `HOW NOT TO FIX (always):` | the library's `AntiFixPolicy` | identical for every rule, immutable |

So rule-specific traps go on the rule. `NoStdoutInServicesRule` names two:

```java
.howNotToFix("""
        Do NOT move the call into a helper class outside ..service.. to dodge the package \
        matcher — the output still lands on stdout. Do NOT swap System.out for a logger you \
        then silence in test configuration.""")
```

The global block cannot be extended, removed, or reworded — that is the point of it. Anything
specific to one rule belongs in that rule's `howNotToFix`.

## Expected output

Adding a second stdout-writing service and running `mvn test` produces this — captured
verbatim, not illustrative:

```text
Architecture Violation [acme.no-stdout-in-services] [Priority: MEDIUM]

WHY:
  Writing to stdout from a service bypasses the logging setup entirely: no level, no correlation id, no way to turn it off in production or capture it in tests.

HOW TO FIX:
  Inject a logger and log at the level the message deserves, or return the value and let the caller decide how to present it.

HOW NOT TO FIX (this rule):
  Do NOT move the call into a helper class outside ..service.. to dodge the package matcher — the output still lands on stdout. Do NOT swap System.out for a logger you then silence in test configuration.

HOW NOT TO FIX (always):
  - Do NOT edit, hand-write, or delete files under archunit/frozen/ to make a NEW violation disappear. The store records pre-existing debt only; new violations must be fixed in code.
  - Do NOT re-run with archunit.freeze.refreeze=true, and do NOT commit freeze.store.default.allowStoreCreation=true. Either one converts every current violation in every rule into accepted debt at once.
  - Do NOT add to or create archunit_ignore_patterns.txt. ArchUnit discards anything matching that file before this rule, the freeze store, or this message ever sees it, leaving no record anywhere. Nothing in this catalog is exempted that way.
  - Do NOT set corral.ignorePatterns.fail=false to make that check go away. It exists to report the file above, so disarming it restores the silence rather than resolving it. That property is for a file you put there deliberately, for your own rules.
  - Do NOT silence the rule with @SuppressWarnings, @ArchIgnore, comments, or by disabling the test.
  - Do NOT rename a class, field, or package solely to dodge a name-based rule (e.g. renaming FooIT so the integration-test rule stops matching).
  - Do NOT narrow @AnalyzeClasses(packages=...) or add ImportOptions to hide code from the scan.
  - Do NOT downgrade, remove, reword, or otherwise weaken the rule.
  - corral-exclusions.txt removes a rule from your build permanently, because it does not apply to this codebase. It is not a way to pass a failing build. Adding a rule to it in the same change that made that rule fail is silencing, not excluding, and reads that way in the diff.
  - The ONLY acceptable resolution is changing the production/test code so the rule genuinely passes — then follow this rule's HOW TO FIX.

EXCLUDED IN THIS BUILD (corral-exclusions.txt — these rules are not enforced here):
  - corral.logging.no-system-err :: Demonstrating exclusion is this module's job; StderrWriterAllowedByExclusion is the demo.

Offending locations:
  Method <com.example.consumer.service.ChattyService.shout(java.lang.String)> calls method <java.io.PrintStream.println(java.lang.String)> in (ChattyService.java:4)
```

Three things to read off it:

- This rule's own guidance sits under "(this rule)". The "(always)" clauses are the immutable
  baseline, identical in every failure — a rule cannot add to them or drop one.
- Only `ChattyService` is reported. `NoisyService` violates the same rule but is frozen, so it stays
  silent — that is freezing working.
- The `EXCLUDED IN THIS BUILD` block appears on *this* rule's failure, though this rule is not the
  excluded one. That is deliberate: an excluded rule never fails, so it can never report itself.

## Excluding a rule

`corral.logging.no-system-err` is frozen **clean** in this store, so `StderrWriterAllowedByExclusion`
writing to stderr is a new violation and fails the build. One committed line keeps it green:

```text
corral.logging.no-system-err :: Demonstrating exclusion is this module's job; StderrWriterAllowedByExclusion is the demo.
```

Delete it and run `mvn clean test` to watch the rule bite — `clean` matters, because Maven copies
test resources into `target/test-classes` but never removes ones you deleted, and Corral reads the
file off the classpath. Without it the stale copy keeps excluding the rule and the build stays
green for the wrong reason. The two edits below change the file rather than remove it, so plain
`mvn test` is enough for those.

Four edits are worth trying while you are in there, because each shows a different edge of the mechanism:

| Edit | What happens |
|---|---|
| Misspell the id (`corral.logging.no-system-errr`) | The build stays green, but logs a warning naming the id: an exclusion that matches no rule removes nothing while reading in the diff as though it did. |
| Drop the ` :: reason` | Every rule fails, naming the file, the line number and the line. A file that is not understood is not trusted to remove anything. |
| Name this module's own `acme.no-stdout-in-services` | Nothing fails and nothing is logged: the file removes any rule that goes through `guard()`, your own included, so `ProjectArchitectureTest` passes with the rule disarmed. Removing a rule you own by not wiring it is the clearer edit. |
| Break any other rule while the exclusion stands | The failure carries an `EXCLUDED IN THIS BUILD` block listing this exclusion — so whoever reads the build sees what is not being enforced. |

Note what does **not** happen: the freeze store is not rewritten. The exclusion wraps the frozen
rule rather than replacing it, so `corral.logging.no-system-err` keeps its `stored.rules` entry and nothing
is deleted. What Corral cannot do is record violations it never evaluated — so anything this module
acquires while the rule is off is *new* the day the line goes away. Exclusion is not a pause button.

## Retiring an id

`retirement/` is the other half of the story: excluding is a *consumer* opting out locally, retiring
is a *catalog owner* withdrawing an id for everyone. This module is both, so it can show both.

`acme.no-java-util-date` shipped, then grew to cover `Calendar` as well as `Date` — at which point
the slug no longer described it. The tempting fix is a rename. **A rename fails silently:** the id is
the freeze-store key, ArchUnit's `ViolationStore` SPI has no rename verb, so every consumer's
recorded violations stay filed under the old key while the rule re-seeds clean and the build goes
green enforcing nothing. `DateRulesGroup` retires it instead:

```java
@ArchTest
static final ArchRule noJavaUtilDate = DeprecatedRule.supersededBy(
        "acme.no-java-util-date",     // retired
        "acme.no-legacy-date-api",    // replacement
        "renamed when it grew to cover Calendar as well as Date, …");
```

Three things to read off the committed store:

- **`acme.no-legacy-date-api` has a `stored.rules` entry and no file.** It is clean here, and the
  entry is what keeps it armed — see [Freezing this rule](#freezing-this-rule).
- **`acme.no-java-util-date` has no entry at all.** A retired id is deliberately never frozen:
  an entry would claim it is enforced, and nothing is enforced under it any more.
- **The retired id is still wired.** Unwired it would register a doc that nothing evaluates. Wired,
  a consumer who excluded the old id keeps building and gets told where the rule went.

Worth trying: add `acme.no-java-util-date :: …` to `corral-exclusions.txt`. It resolves against the
signpost rather than logging the "matched no rule in this run" warning — which is the whole reason a
withdrawn id stays published instead of simply disappearing.

## Freezing this rule

A rule with no `stored.rules` entry **seeds** whatever it finds as debt and passes, which is why
adopting one never fails a build retroactively. `NoisyService` sits in the store on those terms.

The rule is armed from that point on — provided the `stored.rules` line is committed. Uncommitted,
CI keeps seeing a rule with no entry, and every first violation is seeded instead of reported.
