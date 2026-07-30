# Design: Lombok Adoption and DatabaseRules Cleanup

**Date:** 2026-07-30
**Status:** Approved design — ready for implementation planning

---

## Problem

Two unrelated-but-adjacent pieces of work on the `rules` module:

1. **Boilerplate.** `RuleDoc` carries a 33-line hand-written builder; three utility classes each
   carry a private constructor purely to prevent instantiation; the failure formatter declares its
   own SLF4J logger field. Lombok removes all of it.
2. **Debris.** `DatabaseRules` was removed from the library, but its supporting cast was left
   behind: six orphaned test fixtures, a stale README reference, two example-consumer classes that
   existed only to violate the raw-JDBC rule, and — most consequentially — the example's committed
   freeze store is gone, so the example no longer demonstrates the library's central "freeze, don't
   block" property.

## Goal

- Adopt Lombok internally in the `rules` module with **zero change to any call site**.
- Make it impossible for Lombok to silently stop generating code (see § The silent-failure trap).
- Remove every trace of the `DatabaseRules` removal.
- Restore the example consumer's freeze-store demonstration **without adding any dependency to it**.

## Non-goals

- No new rule groups. "Lombok support" here means the library *uses* Lombok, not that it *governs*
  consumers' Lombok usage.
- No change to `maven.compiler.release`, which stays at **25** per an earlier explicit decision.
- No change to any rule id. Ids are freeze-store keys; changing one orphans consumers' frozen
  violations.

---

## Verified facts (spikes already run — do not re-litigate these)

Confirmed empirically against Lombok 1.18.46 on JDK 25 before this design was written.

1. **Lombok 1.18.46 works on JDK 25.** An earlier conclusion that it did not was **wrong**.
2. **The real constraint is javac, not Lombok.** JDK 23+ disabled *implicitly-enabled* annotation
   processing from the classpath (deprecated in 21, then switched off). With Lombok only as a
   `provided` dependency, the processor never runs: `mvn verify` reports **BUILD SUCCESS** while
   `@Getter` generates nothing. Adding `annotationProcessorPaths` to `maven-compiler-plugin` fixes
   it. Observed matrix, same Lombok, same source:

   | Build JDK | Classpath only | With `annotationProcessorPaths` |
   |---|---|---|
   | 21 | works | works |
   | 24 | silently generates nothing | (not retested; irrelevant) |
   | 25 | silently generates nothing | **works** |

   The target `--release` is irrelevant: `--release 21` under JDK 25 is still inert.
3. **`@Builder` works on a record**, and the compact constructor's validation is preserved because
   the generated builder calls the canonical constructor. Verified: a malformed id still throws.
4. **`@Builder` on a record generates `howNotToFix(Optional<String>)`**, not `(String)`. Naive
   adoption would force every call site to write `.howNotToFix(Optional.of("..."))`.
5. **An omitted `Optional` component arrives as `null`, not `Optional.empty()`.** Lombok does not
   default it, so `RuleDoc.builder().id(..).why(..).howToFix(..).build()` — which three existing
   docs and several tests do — would throw `howNotToFix must not be null`. Initialising the field
   in a hand-written partial builder fixes it. **This is the single most breakage-prone detail in
   this design.**
6. **A hand-written partial builder class is merged, not replaced.** Declaring one field and one
   method leaves Lombok to generate the rest.
6a. **Lombok suppresses generation by method *name*, not signature.** Hand-writing
   `howNotToFix(String)` means Lombok generates **no** `howNotToFix(Optional<String>)` overload at
   all — the generated `Builder` contains only `id`, `why`, `howToFix`, the hand-written
   `howNotToFix(String)`, and `build()`. This is what makes the partial-builder approach clean
   rather than leaving a confusing two-overload API. It also means a guard test must anchor on a
   purely-generated member (see § The silent-failure trap).
7. **`@Builder(builderClassName = "Builder")` preserves the `RuleDoc.Builder` type name.** Without
   it Lombok names the class `RuleDocBuilder`, which would break
   `RuleDocTest:12`'s `private static RuleDoc.Builder valid()`.
8. **`@UtilityClass` rejects an already-declared constructor**, failing loudly with
   `@UtilityClasses cannot have declared constructors.` The existing private constructors must be
   deleted, not merely annotated around.
9. **`@UtilityClass` output matches the current hand-written shape** exactly: `public final class`,
   `private` constructor, static members.
10. **`@Slf4j` generates a `private static final org.slf4j.Logger log` field.** `slf4j-api` is
    already a compile-scope dependency of `rules`, so no dependency change is needed.

---

## Part A — Lombok adoption

### Build configuration

- Root `pom.xml`: add `<lombok.version>1.18.46</lombok.version>` and a `dependencyManagement` entry.
- `rules/pom.xml`: add `org.projectlombok:lombok` at **`provided`** scope, so it never reaches
  consumers.
- Root `pom.xml` `pluginManagement`: add `annotationProcessorPaths` to `maven-compiler-plugin`
  listing Lombok. This is **mandatory**, not optional — see verified fact #2.

`annotationProcessorPaths` replaces classpath-based processor discovery. Lombok is the only
processor this build needs, so nothing else must be added to that list. `rules-example` inherits the
configuration harmlessly (it uses no Lombok).

### The silent-failure trap

The failure mode that motivated this section is worth stating plainly, because it is the one thing
most likely to bite a future contributor: **when annotation processing is misconfigured, Lombok
generates nothing and the build still passes.** A class using only `@Getter`/`@Slf4j`, whose
generated members are never referenced from the same module, compiles green and ships a jar that
throws `NoSuchMethodError` in a consumer.

Mitigation: a **guard test** that asserts a Lombok-generated member actually exists at runtime, via
reflection. The anchor must be a member Lombok alone produces — per verified fact #6a, *not*
`howNotToFix`, which is hand-written and therefore present either way. Valid anchors:

- `RuleDoc.Builder` declares `id(String)`, `why(String)`, `howToFix(String)` and `build()`
- `RuleDoc` declares the static `builder()` factory
- `AgentFriendlyFailureDisplayFormat` declares a `private static final org.slf4j.Logger log` field
- `RuleRegistry` is `final` and its only constructor is `private`

Reflection is required rather than a plain compile-time reference, because a compile-time reference
to a missing generated member fails the *build* instead of the *test* — which is a louder failure
but a less informative one. The test must fail if annotation processing is switched off. It plays the
same role for code generation that the deleted `BuildEnvironmentTest` played for the compiler
release.

### Per-class changes

| Class | Change | Boilerplate removed |
|---|---|---|
| `RuleDoc` | `@Builder(builderClassName = "Builder")`; delete `builder()` and most of the hand-written `Builder` | ~27 lines |
| `RuleRegistry` | `@UtilityClass`; delete private constructor | 3 lines |
| `FrozenRules` | `@UtilityClass`; delete private constructor | 3 lines |
| `AntiFixPolicy` | `@UtilityClass`; delete private constructor | 3 lines |
| `AgentFriendlyFailureDisplayFormat` | `@Slf4j`; delete the manual `Logger` field | 2 lines |

`RuleDoc` keeps a partial builder carrying exactly two things, both proven necessary:

```java
public static class Builder {
    // Initialised here so Lombok skips generating the field: without this, an omitted
    // howNotToFix arrives as null and the canonical constructor throws (verified fact #5).
    private Optional<String> howNotToFix = Optional.empty();

    // Keeps the ergonomic String API; Lombok's generated setter would take Optional<String>.
    public Builder howNotToFix(String howNotToFix) {
        this.howNotToFix = Optional.ofNullable(howNotToFix);
        return this;
    }
}
```

The compact constructor, the `ID_PATTERN` validation, and the `requireText` helper all stay
unchanged. Validation continues to run because the generated builder calls the canonical
constructor.

### Deliberately not annotated

`AllCentralRules` and `TestingRules` keep their hand-written private constructors. `@UtilityClass`
rewrites member modifiers, and ArchUnit reflects over these classes' `@ArchTest` fields — that is
not a risk worth taking to delete two constructors. This exclusion is a design decision, not an
oversight.

### Contributor impact

Lombok is a compile-time processor: IDE users need the Lombok plugin, or the IDE reports phantom
errors on generated members. The README gains a one-line note under a Contributing heading.

---

## Part B — Cleanup

### Deletions

- `rules/src/test/java/io/github/milczekt1/archrules/fixtures/database/**` — six files
  (`repository/OrderRepository`, `service/AnnotatedService`, `service/AnnotatedMethodService`,
  `service/CleanService`, `service/RawJdbcService`, `service/SecondAnnotatedService`). Nothing
  references them since `DatabaseRules` and `FreezingBehaviourTest` were removed.
- `rules-example/src/main/java/com/example/consumer/service/ReportService.java` and
  `repository/CustomerRepository.java` — both existed solely to exercise the raw-JDBC rule.
  `GreetingService` stays as the example's benign production class.

### Documentation

`README.md:68` still reads `Opt in group by group instead with ArchTests.in(DatabaseRules.class) /
ArchTests.in(TestingRules.class)`. `DatabaseRules` no longer exists.

**Decision:** keep the sentence and name only `TestingRules` — the group-by-group opt-in is still a
real capability worth documenting, and it stays correct when future groups are added. Do not delete
the sentence. Any other surviving `DatabaseRules` or `db.no-*` reference in the README must go, and
the rules table must list exactly the two surviving `test.*` rules (the drift-guard test that used
to enforce this was removed along with `DatabaseRules`, so this is now a manual check).

### Restoring the example's freeze demonstration

With `DatabaseRules` gone the example has no violation left to freeze, so it no longer shows that
adoption absorbs pre-existing debt — the library's headline property.

Add `rules-example/src/test/java/com/example/consumer/LegacyChecks.java`: a class holding a
`@Test`-annotated method whose simple name ends in neither `Test` nor `IT`. It violates
`test.class-naming-convention`, needs **no new dependency** (JUnit Jupiter arrives transitively via
`archunit-junit5`), and is an authentic instance of the exact bug that rule exists to catch — a test
class the build never runs.

The name must not match Surefire's default includes (`**/Test*.java`, `**/*Test.java`,
`**/*Tests.java`, `**/*TestCase.java`), or Surefire will execute it as a real test. `LegacyChecks`
satisfies that; `LegacyTestCase` would not.

Seed the store once, out of band, then commit it:

```
mvn test -Darchunit.freeze.store.default.allowStoreCreation=true
```

`rules-example/src/test/resources/archunit.properties` already documents this and deliberately does
not pin `allowStoreCreation` — leave that as it is. After seeding, `stored.rules` must contain
`test.class-naming-convention=<uuid>` with the `LegacyChecks` violation recorded against it, and the
other rule's file present but empty.

**A store rewritten by a failing run must never be committed.** If a run fails during seeding,
restore with `git checkout -- rules-example/src/test/resources/archunit/frozen` before retrying.

---

## Testing

- **The existing 49 `rules` tests must pass unchanged.** That is the proof that Lombok adoption
  changed no behaviour and no call site. If any test needs editing, the "zero call-site change"
  claim has failed and the cause should be reported, not worked around.
- **The Lombok guard test** described above.
- `RuleDocTest` in particular exercises the builder's blank-field rejection, id-format rejection,
  and `howNotToFix` defaulting — the three behaviours most at risk from verified facts #4 and #5.
- `rules-example` builds green with the store committed, and its `CentralArchitectureTest` still
  reports 2 tests.
- **Non-default Surefire run orders stay green**: `reversealphabetical`, `alphabetical`, `random`.
  A previous whole-branch review found an order-dependent test that was passing only by filesystem
  luck, so this is a standing requirement, not a one-off.
- Confirm `LegacyChecks` does **not** appear in any surefire report — if it does, it is being run as
  a test and needs renaming.

## Verification

1. `./mvnw -B verify` — full reactor green.
2. `javap -p -cp rules/target/classes io.github.milczekt1.archrules.RuleRegistry` shows a private
   constructor and a final class, proving `@UtilityClass` ran.
3. `cat rules-example/src/test/resources/archunit/frozen/stored.rules` shows
   `test.class-naming-convention=<uuid>`.
4. Temporarily removing `annotationProcessorPaths` makes the guard test fail — proving the guard
   guards. Restore afterwards.
5. `git status --porcelain` clean apart from the pre-existing untracked
   `central-arch-rules-framework-design.patch`.

## Out of scope

- New rule groups, including any group governing consumers' Lombok usage.
- Lowering `maven.compiler.release` below 25.
- The three parked robustness findings from the previous whole-branch review (logger field
  initializer outside the never-throw guard; the raw↔frozen pairing test's missing non-emptiness
  precondition; the store reader's line-break escaping). Note that `@Slf4j` neither fixes nor
  worsens the first: it generates the same static field the manual code declared.
- Cutting a release, CI workflow, sources/javadoc jars, `Automatic-Module-Name`.
