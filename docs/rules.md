# Rules catalog

Every rule Corral publishes. Groups are the unit you wire, one line each from your own root — see the
[quick start](../README.md#quick-start). A group you do not wire is not enforced; a single rule
inside one you do can be switched off with [an exclusion](excluding-a-rule.md). A few rules ship in
no group at all and are wired one at a time — they are in [Opt-in rules](#opt-in-rules) below.

| Rule id | Group | What it enforces |
|---|---|---|
| `corral.test.no-mocked-repository-in-integration-test` | `TestingRulesGroup` | An `*IT` class must not declare a mocked (`@Mock`, `@MockitoBean`, `@MockBean`) field whose type ends in `Repository` or `Dao`. |
| `corral.test.no-thread-sleep` | `TestingRulesGroup` | A test class must not call `Thread.sleep`. Matched by call target owner and name, so every overload counts, and scoped to test classes — a `Sleeper` helper or a `pause()` on a base test class is flagged where it is declared, provided it compiles into test output. `TimeUnit.sleep` and other spellings of a wait are deliberately out of scope: same mistake, different predicate, so they belong under their own freeze-store key. |
| `corral.test.class-names-must-end-with-test-or-it` | `TestingRulesGroup` | A top-level class holding JUnit test methods (`@Test`, `@ParameterizedTest`, `@RepeatedTest`, `@TestFactory`, `@TestTemplate`) must end in `Test`, `Tests` or `IT`. Nested classes — including JUnit 5 `@Nested` groups — are exempt: they run through their enclosing class. |
| `corral.test.no-junit4` | `TestingJunitRulesGroup` | No class compiled into test output — test, helper, fixture or base class alike — may depend on anything `junit:junit` ships: `junit.framework..`, `junit.extensions..`, and `org.junit..` outside `org.junit.jupiter..`, `org.junit.platform..` and `org.junit.vintage..`. One bytecode dependency check, so annotations, superclasses, field and parameter types and `Assert` calls all count. Scoped to test *output* rather than to classes declaring a test: a JUnit 4 `@Before` on an abstract base class silently never runs for its Jupiter subclasses, and the base class is the only place it can be caught. Violations are per-dependency, not per-class. |
| `corral.test.junit.no-disabled-without-reason` | `TestingJunitRulesGroup` | A test class or test method annotated `@Disabled` must state a reason. The bare form, `@Disabled("")` and `@Disabled("   ")` are all flagged, one violation per offending class or method, so a fourth unexplained disable still fails a build that froze the first three. Direct annotations only: a project's own composed annotation — `@DisabledPendingFix`, itself annotated `@Disabled` — is flagged at its *declaration*, where one reason covers every use, and never at a use site, because reaching the `@Disabled` through a meta-annotation is import-scope dependent in ArchUnit and overflows the stack for a consumer who imports broadly. The conditional variants (`@DisabledOnOs`, `@DisabledIfEnvironmentVariable`, `@EnabledIf…`) are separate annotation types and are never matched. Whether the reason is *good* is not checked and is not claimed to be. |
| `corral.test.mockito.no-static-mocking` | `TestingMockitoRulesGroup` | A test class must not install a Mockito static mock. Matched through the `MockedStatic` handle as well as the `Mockito.mockStatic` call, so a base class holding a handle as a field and a helper taking one as a parameter are flagged where they declare it — whenever they compile into test output. The call check covers the remaining shape: a handle handed straight off, so no method of it is called locally. All eight `mockStatic` overloads share the name, so all eight count. Ordinary `Mockito.mock` is untouched, and a base class compiled into main rather than test output is out of scope and says so in the failure text. |
| `corral.test.mockito.no-construction-mocking` | `TestingMockitoRulesGroup` | A test class must not install a Mockito construction mock. The same two checks as the static-mock rule, over `MockedConstruction` and the `Mockito.mockConstruction` call — plus `mockConstructionWithAnswer`, which is a name of its own rather than an overload. Separate id from static mocking deliberately: the two freeze, exclude and retire independently. |
| `corral.logging.no-system-out` | `LoggingRulesGroup` | No class may access `System.out`. Matched as a field access, so every overload of `println`, plus `print`, `printf` and `write`, is covered — static initializers included. |
| `corral.logging.no-system-err` | `LoggingRulesGroup` | No class may access `System.err`. Same field-access match. Kept separate from `corral.logging.no-system-out` so stdout debt and stderr debt freeze under their own keys. `throwable.printStackTrace()` is *not* matched: the field access happens inside `java.lang.Throwable`. |

This table is written by hand and checked by the build: `RulesCatalogDocTest` in `corral-rules`
fails when an id here is not published, a published id is missing here, or a row names the wrong
group. The prose in the third column is not checked.

## Opt-in rules

These ship in the artifact but belong to no group, because a group is adopted whole: everything in
one is on by default for everybody who wires it, and these are rules a team can reasonably decline.
Wire one by naming the rule class, which is exactly what a group's own field does — same freezing,
same exclusions, same failure output.

```java
import io.github.milczekt1.corral.rules.testing.nomutablestaticstate.NoMutableStaticStateRule;

@ArchTest
static final ArchTests noMutableStaticState = ArchTests.in(NoMutableStaticStateRule.class);
```

| Rule id | Wire with | What it enforces |
|---|---|---|
| `corral.test.no-mutable-static-state` | `ArchTests.in(NoMutableStaticStateRule.class)` | A test class must not declare a `static` non-`final` field. JUnit builds a fresh instance per test method to isolate tests from one another, and a static field opts out of that: it carries whatever the last test wrote into it, across methods and across classes in the same fork. `@TempDir` is exempt, because JUnit assigns that field and it therefore cannot be `final`; `@RegisterExtension` and Testcontainers' `@Container` can both be `static final` and are not exempt, so one declared without `final` is flagged and freezes as debt on adoption. Modifiers only — nothing else in the predicate is JUnit-specific, and a test class is identified by its output location or a declared test method, never by its name; compiler-generated fields are not excluded, so a JVM language emitting static non-final fields of its own is flagged on them. A `static final` field holding a mutable object — `static final List<Order> ORDERS = new ArrayList<>()` — is *not* matched and is not made safe by that: it is where this rule's sight ends. |

Opt-in rows are checked by the same test as the table above, on the id set and on the class named in
column 2.

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
