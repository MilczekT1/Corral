|  |  |
|---|---|
| **Rule id** | `corral.spring.no-field-injection` |
| **Proposed group** | `SpringRulesGroup` (**new** — see below) |
| **Rule class** | `rules/spring/nofieldinjection/NoFieldInjectionRule.java` |
| **Priority** | P0 |
| **Effort** | M |

**In one sentence:** A production class must not receive a dependency through an annotated field —
`@Autowired`, `@Inject` and `@Resource` belong on the constructor, where the dependency is required,
final and visible to anyone who calls `new`.

## Why this matters

A field-injected dependency is invisible from outside the class. `new OrderService()` compiles, runs,
and throws `NullPointerException` on the first call, because nothing in the type says an
`OrderRepository` was needed. Every unit test therefore either boots a Spring context to fill the
field, or reaches in with reflection (`ReflectionTestUtils.setField`, `@InjectMocks`) — and a test
that assembles the object by reflection breaks the moment a field is renamed, silently, into a null
that surfaces two assertions later.

The field cannot be `final`, so the object has a window between construction and injection in which it
is half-built, and nothing stops a later assignment. And field injection hides circular dependencies:
Spring resolves a field cycle without complaint, so two services that depend on each other look fine
until somebody tries to test one of them alone. Constructor injection turns that cycle into a boot
failure, which is the point — it is a design problem, surfaced at the cheapest possible moment.

Spring's own reference has recommended constructor injection since 4.3 (2016), when a single
constructor stopped needing `@Autowired` at all. Field injection persists because it is the shape a
decade of tutorials taught, and so it is the shape a coding agent produces by default: `@Autowired
private Foo foo;` is the most common Spring wiring in training data by a wide margin. The fix is not
hard, but it is not obvious either: moving to a constructor can fail the next boot with a
circular-reference error that was not there before, and the tempting move at that point — `@Lazy` on
the parameter — papers over the finding instead of resolving it. That is what makes this rule worth
its failure message, and the reason it is proposed as the catalog's first structural rule rather
than another hygiene check: a rule earns its HOW TO FIX where the fix is not the first thing the
reader would have done anyway.

## Scope of the check, deliberately

**Production classes only.** `@Autowired` on a field of a `@SpringBootTest` class is the documented
idiom and is not flagged. Scoping goes through `TestScope.PRODUCTION_CLASSES`, so it is decided by
test-output location or a declared test method, never by a class name — renaming a service so it
looks like a test does not move it out of scope.

**Annotations matched, by FQN string** so a consumer missing any of these libraries still runs:

- `org.springframework.beans.factory.annotation.Autowired`
- `jakarta.inject.Inject` and `javax.inject.Inject`
- `jakarta.annotation.Resource` and `javax.annotation.Resource`

Listing both `jakarta` and `javax` means the predicate does not have to change when a consumer
migrates, which would otherwise be a freeze-store break for everyone.

**Not matched, on purpose.** `@Value` on a field is the same shape with a different predicate and a
different fix (a constructor parameter carrying `@Value`), so it belongs under its own key — a
candidate `corral.spring.no-field-value-injection`. Setter injection (`@Autowired` on a method) is
likewise a separate rule if ever wanted; it is worse than a constructor but at least declares a
mutator, and mixing it into this key would mean rewording this predicate later. `@Autowired` on a
constructor or on a `@Bean` method parameter is correct usage and is untouched.

## Example violation

```java
// must flag — the dependency is invisible to `new`, mutable, and nullable until Spring runs
@Service
public class OrderService {
    @Autowired
    private OrderRepository orders;
}

// must flag — same shape, JSR-330 spelling
@Component
public class MailSender {
    @Inject
    Session session;
}

// must flag — @Resource on a field is field injection by name
@Component
public class ReportWriter {
    @Resource(name = "reportsDataSource")
    private DataSource dataSource;
}

// must ignore — the dependency is required, final and visible; Spring autowires a single constructor
@Service
public class OrderService {
    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }
}

// must ignore — explicit @Autowired on the constructor Spring should use, out of several
@Service
public class PricingService {
    private final RateSource rates;

    @Autowired
    public PricingService(RateSource rates) { this.rates = rates; }

    PricingService(RateSource rates, Clock clock) { /* for tests */ this.rates = rates; }
}

// must ignore — a test class; field injection is the documented idiom here
@SpringBootTest
class OrderServiceIT {
    @Autowired
    OrderService service;
}

// must ignore — a different predicate, reserved for its own rule (see non-goals)
@Component
public class Greeter {
    @Value("${greeting.prefix}")
    private String prefix;
}
```

## How to fix a violation

Declare a constructor that takes the dependency, assign it to a `private final` field, and delete the
field annotation. With exactly one constructor, Spring uses it without `@Autowired`. Lombok's
`@RequiredArgsConstructor` on the class generates that constructor from the final fields, which makes
the whole change a one-line addition and a one-line removal. If the class has several constructors,
put `@Autowired` on the one Spring should use.

An optional dependency stays in the constructor too: take `Optional<Foo>`, `ObjectProvider<Foo>`, or a
`@Nullable` parameter — the point is that the type declares what it needs.

If the next boot fails with a circular-reference error, the cycle was already there and field
injection was hiding it. Break it: extract the piece both sides need into a third bean, or have one
side publish an event (`ApplicationEventPublisher`) instead of calling back. Then update any test
that assembled the object by reflection to pass its collaborators through the constructor — the test
gets shorter and stops depending on field names.

## How NOT to fix it

Do NOT move the annotation onto a setter. It is the same hidden, mutable, nullable dependency, now
with a public mutator added, and it dodges the predicate without touching the problem. Do NOT
inject `ApplicationContext` or `BeanFactory` and pull the dependency out with `getBean` — in a field,
a `@PostConstruct` method or a static holder — that hides the dependency completely and is the
service-locator anti-pattern this rule exists to keep out. Do NOT add `@Lazy` to a constructor
parameter to silence a circular-reference failure: it wraps a proxy around the cycle and defers the
same problem to the first call, when the stack trace is longer and the context is gone. Do NOT drop
the annotation and leave a non-final field assigned somewhere else — the rule goes quiet and the
object is exactly as half-built as before. Do NOT try to move the class out of scope by naming it
like a test; `TestScope` matches output location and declared test methods, so that renames a
service for nothing. Do NOT add the new violation to the freeze store by hand; the store records what
existed at adoption, not what you just wrote.

## Legitimate exceptions

Test classes, by scope. Abstract base classes that share injected fields across many subclasses are
not an exception: constructor injection works there through `super(...)`, but the migration touches
every subclass, which is exactly what the freeze store is for — adopt now, pay down per class. The
one genuine case is an object Spring does not instantiate but still configures, such as an
`@Aspect` woven by AspectJ load-time weaving and obtained via `aspectOf()`; those need field or
setter injection by construction. They are rare, they are frozen as debt on adoption, and a codebase
built around them can exclude the rule with a reason.

## Catalog criteria

- [x] **Broadly applicable** — Spring's own recommendation since 4.3, true for every Spring codebase, not a house style.
- [x] **Objectively checkable** — a field either carries one of the listed annotations or it does not. Whether the resulting constructor is well-designed is not checked and is not claimed to be.
- [x] **Stable** — the five annotation types have not changed in a decade, and both the `jakarta` and `javax` spellings are listed from the start, so a consumer's migration cannot force a rewording.

## Predicate sketch

```java
/** Matched by FQN string, so a consumer missing any of these libraries still works. */
static final List<String> FIELD_INJECTION_ANNOTATIONS = List.of(
        "org.springframework.beans.factory.annotation.Autowired",
        "jakarta.inject.Inject",
        "javax.inject.Inject",
        "jakarta.annotation.Resource",
        "javax.annotation.Resource");

static final ArchRule DEFINITION = noFields()
        .that().areDeclaredInClassesThat(TestScope.PRODUCTION_CLASSES)
        .should().beAnnotatedWith(anyOf(FIELD_INJECTION_ANNOTATIONS));

// anyOf: a DescribedPredicate<JavaAnnotation<?>> that is true when the annotation's raw type name
// is in the list. Described as "a field injection annotation" so the store key reads well.
```

Three implementation notes:

- **Per-field violations, one line each.** A class with three injected fields freezes as three lines,
  so a fourth added later is visible. This is the reason for `noFields()` over a per-class
  `ArchCondition`, which would collapse them into one line and hide the fourth (the reporting trap
  #40 notes for `@Disabled`).
- **`TestScope.PRODUCTION_CLASSES` is the scope, not a name pattern.** The description text of that
  predicate renders into this rule's freeze-store key, so this rule inherits the same constraint every
  other `TestScope` consumer has: rewording `TestScope` re-seeds every store.
- **Examples need Spring and JSR-330 on the test classpath of `corral-rules`**, the way `junit:junit`
  is there for `corral.test.no-junit4`: `spring-beans` (already managed via `spring.version`),
  `jakarta.inject-api`, `jakarta.annotation-api`, and `javax.inject:javax.inject:1`, all test-scoped,
  with the same one-line comment in the parent POM explaining why they exist.

Inspects production classes, so it holds whether or not consumers set `ImportOption.DoNotIncludeTests`.

## Non-goals

- **`@Value` on fields.** Same shape, different fix, own key. Proposed separately if wanted.
- **Setter injection.** A different predicate, and a different argument; own key if ever wanted.
- **Judging the constructor.** A constructor with twelve parameters passes this rule. That is a
  design smell the rule surfaces by making it visible, not one it claims to check.

## Examples

- **Must flag:** `@Autowired` on a private field of a `@Service`; `@Inject` on a package-private field of a `@Component`; `@Resource(name = …)` on a field; the same three on a field of a plain class with no stereotype annotation at all (the wiring is wrong regardless of how the bean is registered).
- **Must ignore:** a `private final` field assigned in the only constructor; `@Autowired` on a constructor; `@Autowired` on a `@Bean` method parameter; `@Autowired` on a field of a class in test output; `@Value` on a field; a `static` field with no injection annotation.

## Group change this rule carries with it

This rule ships in a **new `SpringRulesGroup`**, the first group whose rules assume a framework. The
split is the same one #40 draws for JUnit: a consumer not on Spring never wires the group and never
sees the rule, while `TestingRulesGroup` and `LoggingRulesGroup` keep holding for everyone. No
existing rule moves, so nothing about this is a breaking change.

Wiring, on top of the usual rule steps:

- [ ] New `groups/SpringRulesGroup.java` — `@UtilityClass`, one `@ArchTest ArchTests` field (`noFieldInjection`).
- [ ] Add an `@ArchTest ArchTests` field for the new group to `EveryPublishedGroup` (test sources) — without it, `PublishedCatalogTest.everyPublishedGroupIsReachableFromHere` fails.
- [ ] Add `corral.spring.no-field-injection` to the expected set in `PublishedCatalogTest.ruleDiscoveryDescendsThroughNestedGroups`.
- [ ] Parent POM: `spring-beans`, `jakarta.inject-api`, `jakarta.annotation-api`, `javax.inject` in `dependencyManagement`; `corral-rules` POM: the same four at test scope, commented like `junit4.version`.
- [ ] `docs/rules.md` — the new rule's row and the new group.
- [ ] README — catalog count and the group list in the quick start.
- [ ] Committed freeze store: `corral-rules/src/test/resources/archunit/frozen/corral.spring.no-field-injection` plus its `stored.rules` line.

`spring` is already a segment-2 concern in `RuleIdGrammarTest`, so the id needs no vocabulary change.

---

Drafted 2026-09-16, after a twelve-run experiment (three models, two effort levels, with and without
the agent-friendly failure format) in which every agent fixed `corral.logging.no-system-out` and
`corral.test.no-thread-sleep` honestly and without needing the guidance — the fixes were obvious.
This rule is proposed as the first whose HOW TO FIX carries information the reader would not have
had: the circular-reference failure a constructor surfaces, and the `@Lazy` trap that follows. This
file is the draft; the GitHub issue created from it will be the live record.
