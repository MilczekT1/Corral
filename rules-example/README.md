# Example consumer

A working consumer of `llama-rules`: one test-scoped dependency, the library's rules wired in, and a
rule this project owns.

| File | What it shows |
|---|---|
| `CentralArchitectureTest` | Wiring the library's rules — the whole point of the dependency. |
| `custom/NoStdoutInServices` | Writing **your own** rule with the library's machinery, and appending a clause to the anti-fix policy. |
| `custom/CustomArchitectureTest` | Wiring your own rules alongside the library's. |
| `archunit/frozen/` | The committed freeze store. Three entries, two files — the clean rule has no file. |
| `InvalidlyNamedTestClass`, `service/NoisyService` | Deliberate, permanent violations, frozen as debt. |

## Writing your own rule

Any `ArchRule` works with `@ArchTest`. But a plain one renders through ArchUnit's default format —
one line, no guidance:

```
Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package '..service..'
should call method PrintStream.println(String)' was violated (1 times): …
```

Give it a `RuleDoc` and freeze it through `FrozenRules`, as `NoStdoutInServices` does, and it behaves
like a built-in rule: guidance on failure, and existing violations recorded as debt instead of
blocking the build.

```java
static final RuleDoc DOC = RuleDoc.builder()
        .id("acme.no-stdout-in-services")
        .why(...)
        .howToFix(...)
        .howNotToFix(...)
        .build();

static final ArchRule RULE = noClasses()
        .that().resideInAPackage("..service..")
        .should().callMethod(PrintStream.class, "println", String.class);

@ArchTest
public static final ArchRule rule = FrozenRules.freeze(RULE, DOC);
```

Ids are freeze-store keys, so use your own namespace (`acme.`) and treat a rename as a breaking
change.

## Extending the anti-fix policy

`AntiFixPolicy.addClause(...)` appends to the "HOW NOT TO FIX (always):" block printed on **every**
rule failure in the build — the library's rules included, not only your own. `NoStdoutInServices`
registers its clause from a static initialiser:

```java
static {
    AntiFixPolicy.addClause(
            "Do NOT swap System.out for a logger you then silence in test configuration.");
}
```

The baseline clauses cannot be removed or replaced, only appended to.

> A static initialiser runs when its class is first loaded, so a clause registered this way is
> guaranteed to be present for that rule's own failures, but a rule in a class loaded earlier may
> render without it. Put clauses that must appear everywhere in a class you know is loaded first, or
> accept that they are best-effort for unrelated rules.

## Expected output

Adding a second stdout-writing service and running `mvn test` produces this — captured verbatim,
not illustrative:

```
Architecture Violation [acme.no-stdout-in-services] [Priority: MEDIUM]

WHY:
  Writing to stdout from a service bypasses the logging setup entirely: no level, no correlation id, no way to turn it off in production or capture it in tests.

HOW TO FIX:
  Inject a logger and log at the level the message deserves, or return the value and let the caller decide how to present it.

HOW NOT TO FIX (this rule):
  Do NOT move the call into a helper class outside ..service.. to dodge the package matcher — the output still lands on stdout.

HOW NOT TO FIX (always):
  - Do NOT edit, hand-write, or delete files under archunit/frozen/ to make a NEW violation disappear. The store records pre-existing debt only; new violations must be fixed in code.
  - Do NOT silence the rule with @SuppressWarnings, @ArchIgnore, comments, or by disabling the test.
  - Do NOT rename a class, field, or package solely to dodge a name-based rule (e.g. renaming FooIT so the integration-test rule stops matching).
  - Do NOT narrow @AnalyzeClasses(packages=...) or add ImportOptions to hide code from the scan.
  - Do NOT downgrade, remove, reword, or otherwise weaken the rule.
  - The ONLY acceptable resolution is changing the production/test code so the rule genuinely passes — then follow this rule's HOW TO FIX.
  - Do NOT swap System.out for a logger you then silence in test configuration.

Offending locations:
  Method <com.example.consumer.service.ChattyService.shout(java.lang.String)> calls method <java.io.PrintStream.println(java.lang.String)> in (ChattyService.java:5)
```

Two things to read off it:

- The last "always" clause is the one `addClause` added. The six above it are the baseline.
- Only `ChattyService` is reported. `NoisyService` violates the same rule but is frozen, so it stays
  silent — that is freezing working.

## Freezing this rule

`acme.no-stdout-in-services` was not in `stored.rules` when it was first added, so its first run
**seeded** `NoisyService` as debt and passed. That is by design, and it is why a new rule never
retroactively fails a build. The rule is armed from that run onward — provided the resulting
`stored.rules` line is committed.
