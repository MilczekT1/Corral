# Creating a rule

**You are adding a rule to Corral's catalog.**

Not what you want? See [excluding a rule](excluding-a-rule.md) or
[retiring a rule](retiring-a-rule.md). Writing rules for *your own* project instead? Depend on
`corral-sdk` alone and use your own namespace — you need none of this.

> Working in Claude Code? The `adding-a-rule` skill walks these steps interactively.

## 1. Choose the id

`corral.<concern>.<slug>` — see [Rule ids](../CONTRIBUTING.md#rule-ids) for the vocabulary and caps.

The only real judgement is the concern: **name the library whose *correct use* the rule asserts.** A
library named only to *detect* something doesn't count — a rule flagging test libraries in production
code names JUnit and Mockito to find them, so it stays `layering`, not `junit`.

## 2. Write the rule class

`corral-rules/src/main/java/.../rules/<topic>/<Name>Rule.java` — `final class implements
DocumentedRule`, private constructor, name ends in `Rule`.

**Field order is load-bearing:**

```java
static final RuleDoc DOC = ...;
static final ArchRule DEFINITION = ...;          // raw, package-private

@ArchTest
public static final ArchRule rule = new NoSystemOutRule().guard();   // last
```

## 3. Fixtures and test

Fixtures go under `corral-rules/src/test/java/.../fixtures/<topic>/` — at least one class the rule
must **flag** and one it must **ignore**. Surefire excludes `**/fixtures/**`, so fixtures named
`*Test`/`*IT` don't run themselves.

Test against the raw `DEFINITION`, asserting **both** directions.

## 4. Register it

| Step | Where |
|---|---|
| Add an `@ArchTest ArchTests` field | the group — a rule nobody points at is never run |
| Add the id | `corral-rules/src/test/resources/published-rule-ids.txt` |
| Extend the expected set | `AllCentralRulesTest.ruleDiscoveryDescendsThroughNestedGroups` |
| Add a row | the [Rules table](../README.md#rules) — nothing enforces this |

## ⚠️ Three ways to ship a rule that enforces nothing

| Mistake | Why it's silent |
|---|---|
| `@ArchTest` field declared **above** `DOC`/`DEFINITION` | `guard()` runs at class init and reads them — they're still `null`. Only *fields* initialise in order; method order is irrelevant |
| Testing the **published** field instead of `DEFINITION` | The published field is frozen: it seeds on first run and passes. The test proves nothing |
| Consumers setting `ImportOption.DoNotIncludeTests` | Every test-scope rule passes vacuously |

Each gives you a green build and zero enforcement.

## Is it catalog-worthy?

Broadly applicable, objectively checkable, and stable — see
[Is a rule catalog-worthy?](../CONTRIBUTING.md#is-a-rule-catalog-worthy). A rule encoding one team's
preference belongs in that team's own namespace.
