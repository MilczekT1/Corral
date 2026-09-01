# Rules catalog

Every rule Corral publishes. Groups are the unit you wire, one line each from your own root — see the
[quick start](../README.md#quick-start). A group you do not wire is not enforced; a single rule
inside one you do can be switched off with [an exclusion](excluding-a-rule.md).

| Rule id | Group | What it enforces |
|---|---|---|
| `corral.test.no-mocked-repository-in-integration-test` | `TestingRulesGroup` | An `*IT` class must not declare a mocked (`@Mock`, `@MockitoBean`, `@MockBean`) field whose type ends in `Repository` or `Dao`. |
| `corral.test.no-thread-sleep` | `TestingRulesGroup` | A test class must not call `Thread.sleep` or `TimeUnit.sleep`. Matched by call target owner and name, so every overload counts, and scoped to test classes — a `Sleeper` helper or a `pause()` on a base test class is test code too, and is flagged where it is declared. |
| `corral.test.class-names-must-end-with-test-or-it` | `TestingRulesGroup` | A top-level class holding JUnit test methods (`@Test`, `@ParameterizedTest`, `@RepeatedTest`, `@TestFactory`, `@TestTemplate`) must end in `Test`, `Tests` or `IT`. Nested classes — including JUnit 5 `@Nested` groups — are exempt: they run through their enclosing class. |
| `corral.logging.no-system-out` | `LoggingRulesGroup` | No class may access `System.out`. Matched as a field access, so every overload of `println`, plus `print`, `printf` and `write`, is covered — static initializers included. |
| `corral.logging.no-system-err` | `LoggingRulesGroup` | No class may access `System.err`. Same field-access match. Kept separate from `corral.logging.no-system-out` so stdout debt and stderr debt freeze under their own keys. `throwable.printStackTrace()` is *not* matched: the field access happens inside `java.lang.Throwable`. |

This table is maintained by hand; nothing in the build checks it.

## Rule ids

**Rule ids are freeze-store keys.** Changing an id orphans every consumer's frozen entry, so treat it
as a breaking change.

Every id Corral publishes starts with the `corral.` vendor prefix, so it can never collide with a
namespace you pick for your own rules — `acme.no-stdout-in-services` in the
[example consumer](../corral-example) shows the generic namespace this frees up. After the prefix, an
id is a dot-namespaced, kebab-cased shape, and every slug carries exactly one of two markers: `no-`
for a prohibition (`corral.logging.no-system-out`) or `-must-` for an obligation
(`corral.test.class-names-must-end-with-test-or-it`). Ids are never renamed, only deprecated — the
old one stays registered, always passing, naming its replacement. The full grammar and the reason
renaming is unsafe are in [CONTRIBUTING.md § Rule ids](../CONTRIBUTING.md#rule-ids).
