# Rule-Per-Class Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make each rule a self-contained class, turn groups into thin composable wrappers, stop the freeze store writing empty violation files, and untrack `docs/superpowers/`.

**Architecture:** A rule class owns its `RuleDoc`, its raw predicate and its frozen `@ArchTest ArchRule`. A group owns only a `MEMBERS` list plus one `@ArchTest ArchTests` field per member, so groups nest into groups to any depth. A `ViolationStore` decorator wraps ArchUnit's `TextFileBasedViolationStore`, keeping the `stored.rules` index entry (which is what keeps a rule frozen) while deleting the file when a rule is clean.

**Tech Stack:** Java 25, Maven, ArchUnit 1.4.2, JUnit Jupiter 5.11.3, Lombok 1.18.46.

**Source spec:** `docs/superpowers/specs/2026-07-30-rule-per-class-architecture-design.md`

---

## Verified facts (spikes already run — do not re-litigate)

1. **Three-level `ArchTests` nesting works** on ArchUnit 1.4.2. Aggregator → group → rule class resolves; the JUnit tree renders `AllRules > TopicRules > NamingRule > rule`, and the rule genuinely evaluates (a deliberate spike violation was reported).
2. **A custom store is registered by property:** `freeze.store=<fqcn>` in `archunit.properties`, instantiated reflectively — so it needs a **public no-arg constructor**.
3. **`TextFileBasedViolationStore` is `public final`**, so the decorator delegates rather than reimplementing.
4. **`ViolationStore` has exactly four methods:** `initialize(Properties)`, `contains(ArchRule)`, `save(ArchRule, List<String>)`, `getViolations(ArchRule)`.
5. **An unknown rule seeds and passes.** `FreezingArchRule.evaluate` does `if (!store.contains(delegate) || refreezeViolations()) return storeViolationsAndReturnSuccess(result);`. Dropping a clean rule's index entry would therefore make its first real violation get absorbed as debt **silently**. This is why the entry must survive.

---

## Global Constraints

- **No rule id changes.** Ids are freeze-store keys; changing one orphans consumers' frozen violations. The published set stays exactly `test.class-naming-convention` and `test.no-mocked-repository-in-integration-test`.
- **No behaviour change to any rule.** This is a move, not a rewrite. Existing rule tests must pass against the moved predicates; a rule that changes behaviour during a move is a defect.
- `maven.compiler.release` stays **25**. Lombok wiring stays as-is.
- No changes to `RuleDoc`, `RuleRegistry`, `FrozenRules`, or `AgentFriendlyFailureDisplayFormat`.
- Every task ends with the reactor green. No task may leave the build red for a later task to fix.
- Non-default Surefire run orders must stay green (`reversealphabetical` at minimum) — an earlier review found an order-dependent test passing only by filesystem luck.

---

## File Structure

```text
rules/src/main/java/io/github/milczekt1/archrules/
  rules/testing/
    NoMockedRepositoryInIntegrationTest.java   # CREATE: rule + doc + frozen field
    TestClassNamingConvention.java             # CREATE: rule + doc + frozen field
  groups/
    TestingRules.java                          # REWRITE: thin wrapper, MEMBERS + ArchTests fields
    AllCentralRules.java                       # MODIFY: groups() -> members(), recursive loadAll()
  freeze/
    EmptyOmittingViolationStore.java           # CREATE: ViolationStore decorator
rules/src/test/java/io/github/milczekt1/archrules/
  rules/testing/
    NoMockedRepositoryInIntegrationTestTest.java  # CREATE: from TestingRulesTest
    TestClassNamingConventionTest.java            # CREATE: from TestingRulesTest
  groups/
    GroupMembership.java                       # CREATE: reusable membership guard helper
    TestingRulesTest.java                      # REWRITE: membership guard only
    AllCentralRulesTest.java                   # MODIFY: recursive, members()
    RuleRegistryCompletenessTest.java          # MODIFY: recursive rule discovery
    TestingRulesFrozenFieldsTest.java          # MOVE: follows its rules
    FrozenFieldStores.java                     # MODIFY: reachable from rule packages
  testsupport/
    PublishedRules.java                        # MODIFY: recursive descent through nested groups
  freeze/
    EmptyOmittingViolationStoreTest.java       # CREATE
rules-example/src/test/resources/archunit.properties  # MODIFY: freeze.store=
.gitignore                                     # MODIFY: docs/superpowers/
```

**Decomposition rationale:** tooling is generalised *before* rules move (Task 1), because `AllCentralRulesTest.everyExposedGroupContributesAtLeastOneRule` currently asserts groups hold `@ArchTest ArchRule` fields directly — the moment rules move behind `ArchTests`, that assertion breaks. Generalising first keeps every task green. Tasks 3–5 are independent of 1–2 and of each other.

---

## Task 1: Generalise tooling for nested groups

**Files:**
- Modify: `rules/src/test/java/io/github/milczekt1/archrules/testsupport/PublishedRules.java`
- Modify: `rules/src/main/java/io/github/milczekt1/archrules/groups/AllCentralRules.java`
- Modify: `rules/src/test/java/io/github/milczekt1/archrules/groups/AllCentralRulesTest.java`
- Modify: `rules/src/test/java/io/github/milczekt1/archrules/groups/RuleRegistryCompletenessTest.java`

**Interfaces:**
- Consumes: nothing new.
- Produces: `PublishedRules` that discovers rules through arbitrarily nested `ArchTests`; `AllCentralRules.members()` (renamed from `groups()`) and a `loadAll()` that recurses. Tasks 2 and 4 depend on this.

> **Why first.** No rule moves in this task. It teaches the tooling to handle a shape that does not
> exist yet, so that Task 2's move is a pure move. Doing it the other way round leaves Task 1 red.

- [ ] **Step 1: Write the failing test**

Add to `AllCentralRulesTest`, which currently only understands one level:

```java
    @Test
    void ruleDiscoveryDescendsThroughNestedGroups() {
        // A group whose members are themselves groups (or rule classes reached via ArchTests) must
        // still yield its rules. Before nesting support this returns empty for anything but a group
        // that declares @ArchTest ArchRule fields directly.
        Set<String> ids = PublishedRules.idSet();

        assertEquals(Set.of(
                "test.class-naming-convention",
                "test.no-mocked-repository-in-integration-test"), ids);
    }
```

Also replace `everyExposedGroupContributesAtLeastOneRule`'s body so it counts rules **recursively**:

```java
    @Test
    void everyExposedMemberContributesAtLeastOneRule() {
        for (Class<?> member : classesExposedToConsumers()) {
            assertFalse(PublishedRules.rulesReachableFrom(member).isEmpty(),
                    member.getSimpleName() + " is aggregated but publishes no rule, so consumers "
                            + "evaluate an empty node");
        }
    }
```

- [ ] **Step 2: Run to verify it fails**

Run: `./mvnw -B -pl rules test -Dtest=AllCentralRulesTest`

Expected: FAIL — `cannot find symbol: method rulesReachableFrom`. That compile failure is the RED for this step; capture it.

- [ ] **Step 3: Make `PublishedRules` recursive**

Add to `PublishedRules`:

```java
    /**
     * Every {@code @ArchTest ArchRule} reachable from {@code root}, descending through any
     * {@code @ArchTest ArchTests} fields. A rule class is a leaf (it declares ArchRule fields); a
     * group is a branch (it declares ArchTests fields). Both shapes, and any nesting of them, are
     * handled by the same walk.
     */
    public static List<ArchRule> rulesReachableFrom(Class<?> root) {
        List<ArchRule> collected = new ArrayList<>(archRuleFieldsOf(root));
        for (ArchTests nested : archTestsFieldsOf(root)) {
            collected.addAll(rulesReachableFrom(nested.getDefinitionLocation()));
        }
        return collected;
    }
```

Then change `idSet()` (and whatever currently walks one level) to start from `AllCentralRules` and use `rulesReachableFrom`, rather than iterating `groups()` and reading `archRuleFieldsOf` directly.

`getDefinitionLocation()` is `@Internal` — the existing Javadoc in `AllCentralRulesTest` already explains why reading it is acceptable and confined to tests. Keep that reasoning; it now applies to `PublishedRules` too, so move or copy the note there.

- [ ] **Step 4: Rename `groups()` to `members()`**

In `AllCentralRules`: rename `GROUPS` to `MEMBERS`, `groups()` to `members()`, and update the Javadoc to say a member may be a group **or** a rule class. Make `loadAll()` recurse:

```java
    public static void loadAll() {
        loadAll(MEMBERS);
    }

    private static void loadAll(List<Class<?>> members) {
        for (Class<?> member : members) {
            try {
                Class.forName(member.getName(), true, member.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Could not load rule member " + member.getName(), e);
            }
        }
    }
```

Nested groups are reached because loading a group runs its static initialisers, which construct its `ArchTests` fields, which in turn load the classes they point at. If a later task shows a nested group's docs are missing from the registry, make the recursion explicit rather than relying on that — and say so in the report.

Update every call site: `AllCentralRulesTest`, `RuleRegistryCompletenessTest`, `PublishedRules`, and the README growth-path checklist if it names `groups()`.

- [ ] **Step 5: Run to verify it passes**

Run: `./mvnw -B -pl rules test`

Expected: PASS — same test count as before plus the one new test, 0 failures. The published id set is unchanged.

- [ ] **Step 6: Commit**

```bash
git add rules/src
git commit -m "refactor: make rule discovery recurse through nested groups"
```

---

## Task 2: Extract the two rule classes

**Files:**
- Create: `rules/src/main/java/io/github/milczekt1/archrules/rules/testing/NoMockedRepositoryInIntegrationTest.java`
- Create: `rules/src/main/java/io/github/milczekt1/archrules/rules/testing/TestClassNamingConvention.java`
- Rewrite: `rules/src/main/java/io/github/milczekt1/archrules/groups/TestingRules.java`
- Create: `rules/src/test/java/io/github/milczekt1/archrules/rules/testing/NoMockedRepositoryInIntegrationTestTest.java`
- Create: `rules/src/test/java/io/github/milczekt1/archrules/rules/testing/TestClassNamingConventionTest.java`
- Create: `rules/src/test/java/io/github/milczekt1/archrules/groups/GroupMembership.java`
- Rewrite: `rules/src/test/java/io/github/milczekt1/archrules/groups/TestingRulesTest.java`
- Modify: `rules/src/test/java/io/github/milczekt1/archrules/groups/TestingRulesFrozenFieldsTest.java`, `FrozenFieldStores.java`

**Interfaces:**
- Consumes: Task 1's recursive tooling.
- Produces: `rules.testing.NoMockedRepositoryInIntegrationTest` and `rules.testing.TestClassNamingConvention`, each with package-private `DOC` and `RULE` and a public `@ArchTest ArchRule rule`; `groups.TestingRules` exposing `members()` and one `@ArchTest ArchTests` field per rule.

> **This is a move, not a rewrite.** Copy each rule's `RuleDoc` prose, predicate and constants
> **verbatim**. The prose was reviewed and, in one case, ruled on by the project owner. If you find
> yourself improving wording or simplifying a predicate, stop — that is a different change.

- [ ] **Step 1: Create the two rule classes**

For each rule, move from `TestingRules` verbatim into its own class:

| New class | Moves in |
|---|---|
| `NoMockedRepositoryInIntegrationTest` | `FORBIDDEN_MOCK_ANNOTATIONS`, `NO_MOCKED_REPOS_IN_IT_DOC`, `NO_MOCKED_REPOS_IN_IT_RULE`, the `declareAMockedRepositoryOrDaoField()` condition, and the `@ArchTest` field |
| `TestClassNamingConvention` | `JUNIT_TEST_ANNOTATIONS`, `TEST_NAMING_DOC`, `TEST_NAMING_RULE`, and the `@ArchTest` field |

Shape for each:

```java
public final class TestClassNamingConvention {

    static final List<String> JUNIT_TEST_ANNOTATIONS = /* verbatim */;

    static final RuleDoc DOC = /* verbatim, id unchanged */;

    static final ArchRule RULE = /* verbatim */;

    @ArchTest
    public static final ArchRule rule = FrozenRules.freeze(RULE, DOC);

    private TestClassNamingConvention() {
    }
}
```

Rename the constants to `DOC` and `RULE` — inside a class named after the rule, the long prefixes carry no information. Keep the rule **id** string byte-identical.

Carry each constant's existing Javadoc with it, especially `JUNIT_TEST_ANNOTATIONS`' note about `isAnnotatedWith` seeing direct annotations only — that comment records why every annotation is listed explicitly and is the reason a past false negative was caught.

- [ ] **Step 2: Rewrite `TestingRules` as a wrapper**

```java
public final class TestingRules {

    private static final List<Class<?>> MEMBERS = List.of(
            NoMockedRepositoryInIntegrationTest.class,
            TestClassNamingConvention.class);

    @ArchTest
    public static final ArchTests noMockedRepositoryInIntegrationTest =
            ArchTests.in(NoMockedRepositoryInIntegrationTest.class);

    @ArchTest
    public static final ArchTests testClassNamingConvention =
            ArchTests.in(TestClassNamingConvention.class);

    /** Members may be rule classes or nested groups. @see AllCentralRules#members() */
    public static List<Class<?>> members() {
        return MEMBERS;
    }

    private TestingRules() {
    }
}
```

Keep the existing class Javadoc paragraph about consumers not configuring `ImportOption.DoNotIncludeTests` — it is guidance about this group's rules and still belongs here.

- [ ] **Step 3: Split the rule tests**

`TestingRulesTest` currently holds tests for both rules plus fixtures shared between them. Split by rule:

- Tests naming `MockingRepositoryIT`, `MockingDaoIntegrationTest`, `MockingGatewayIT`, `PlainUnitTest`, and the `FORBIDDEN_MOCK_ANNOTATIONS` coverage test → `NoMockedRepositoryInIntegrationTestTest`
- Tests naming `BadlyNamedTestCase`, `BadlyNamedParameterizedCase`, `NestedGroupsTest`, `WellNamedTest`, and the `JUNIT_TEST_ANNOTATIONS` coverage test → `TestClassNamingConventionTest`

Each new test lives in `io.github.milczekt1.archrules.rules.testing` so it can read its rule's package-private `RULE` and `DOC`. Move assertions **verbatim**; only the class they reference changes (`TestingRules.TEST_NAMING_RULE` becomes `TestClassNamingConvention.RULE`). Fixtures stay where they are — they are shared and already excluded from Surefire.

Delete `TestingRulesTest`'s rule assertions; what remains becomes the membership guard in Step 4.

- [ ] **Step 4: Add a reusable membership guard**

Create `rules/src/test/java/io/github/milczekt1/archrules/groups/GroupMembership.java`:

```java
/**
 * Shared assertions for the group contract: a group declares each member twice — in {@code
 * members()} for tooling, and as an {@code @ArchTest ArchTests} field for the engine. Divergence is
 * silent in the worst direction: a member present only in {@code members()} is documented and
 * completeness-checked but never evaluated by any consumer.
 */
final class GroupMembership {

    static void assertMembersMatchArchTestsFields(Class<?> group, List<Class<?>> members) {
        Set<Class<?>> exposed = PublishedRules.archTestsFieldsOf(group).stream()
                .map(ArchTests::getDefinitionLocation)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertEquals(Set.copyOf(members), exposed,
                group.getSimpleName() + ": members() and @ArchTest ArchTests fields have diverged; "
                        + "every member needs BOTH");
        assertEquals(members.size(), PublishedRules.archTestsFieldsOf(group).size(),
                group.getSimpleName() + ": one @ArchTest ArchTests field per member, no duplicates");
    }

    private GroupMembership() {
    }
}
```

Rewrite `TestingRulesTest` to a single test calling
`GroupMembership.assertMembersMatchArchTestsFields(TestingRules.class, TestingRules.members())`, and
refactor `AllCentralRulesTest`'s two divergence tests to call the same helper. That is the guard the
spec requires at every nesting level, written once.

- [ ] **Step 5: Point the frozen-field tests at the rule classes**

`TestingRulesFrozenFieldsTest` pairs each public frozen field with its raw rule. Move it to
`io.github.milczekt1.archrules.rules.testing` (it needs package-private access to `RULE`) and update
it to reference the rule classes. `FrozenFieldStores` stays a shared helper; widen its visibility to
`public` if the move puts it in a different package.

- [ ] **Step 6: Run the tests**

Run: `./mvnw -B -pl rules test`

Expected: PASS with the **same total count** as after Task 1 — tests moved, none lost. If the count
dropped, a test was dropped in the split; find it rather than accepting the new number.

Then confirm the tree shape:

Run: `./mvnw -B -pl rules test -Dtest=AllCentralRulesTest`

Expected: PASS, published id set still exactly the two ids.

- [ ] **Step 7: Verify the nesting resolves for a consumer**

Run: `./mvnw -B verify`

Expected: PASS, `rules-example` still reports 2 tests — proof that `AllCentralRules → TestingRules → rule class` resolves through a real `@AnalyzeClasses` consumer, not just in the library's own reflection tests.

- [ ] **Step 8: Commit**

```bash
git add rules/src
git commit -m "refactor: give each rule its own class, make groups thin wrappers"
```

---

## Task 3: `EmptyOmittingViolationStore`

**Files:**
- Create: `rules/src/main/java/io/github/milczekt1/archrules/freeze/EmptyOmittingViolationStore.java`
- Test: `rules/src/test/java/io/github/milczekt1/archrules/freeze/EmptyOmittingViolationStoreTest.java`

**Interfaces:**
- Consumes: nothing from Tasks 1–2; independent.
- Produces: `public class EmptyOmittingViolationStore implements ViolationStore` with a **public no-arg constructor** (ArchUnit instantiates it reflectively from `freeze.store`). Task 4 wires it into the example.

> **The invariant that must not break.** The `stored.rules` entry is what keeps a rule frozen.
> `FreezingArchRule` treats "store does not contain this rule" as *seed the violations and pass*, so
> if this decorator ever drops an index entry, that rule's first real violation is absorbed as debt
> and the build stays green. Delete the **file**, never the entry.

- [ ] **Step 1: Write the failing test**

Create `EmptyOmittingViolationStoreTest`. It drives the store directly rather than through
`FreezingArchRule`, so each behaviour is isolated:

```java
class EmptyOmittingViolationStoreTest {

    @TempDir
    Path storeDir;

    private EmptyOmittingViolationStore store;

    private static ArchRule ruleNamed(String description) {
        return classes().should().haveSimpleName("Whatever").as(description).allowEmptyShould(true);
    }

    @BeforeEach
    void initStore() {
        Properties properties = new Properties();
        properties.setProperty("default.path", storeDir.toString());
        properties.setProperty("default.allowStoreCreation", "true");
        properties.setProperty("default.allowStoreUpdate", "true");
        store = new EmptyOmittingViolationStore();
        store.initialize(properties);
    }

    @Test
    void aCleanRuleIsRecordedInTheIndexButLeavesNoFile() throws Exception {
        ArchRule rule = ruleNamed("test.clean-rule");

        store.save(rule, List.of());

        assertTrue(store.contains(rule), "a clean rule must still be frozen");
        String index = Files.readString(storeDir.resolve("stored.rules"));
        assertTrue(index.contains("test.clean-rule="), "index entry must survive: " + index);
        assertEquals(1, countViolationFiles(), "a clean rule must leave no violation file");
    }

    @Test
    void aCleanRuleReadsBackAsZeroViolations() {
        ArchRule rule = ruleNamed("test.clean-rule");
        store.save(rule, List.of());

        assertEquals(List.of(), store.getViolations(rule));
    }

    @Test
    void aViolatingRuleKeepsItsFile() throws Exception {
        ArchRule rule = ruleNamed("test.dirty-rule");

        store.save(rule, List.of("Class <Foo> is bad"));

        assertEquals(List.of("Class <Foo> is bad"), store.getViolations(rule));
        assertEquals(2, countViolationFiles(), "violations must be written to a file");
    }

    @Test
    void aRuleThatBecomesCleanLosesItsFileButKeepsItsEntry() throws Exception {
        ArchRule rule = ruleNamed("test.was-dirty");
        store.save(rule, List.of("Class <Foo> is bad"));

        store.save(rule, List.of());

        assertTrue(store.contains(rule));
        assertEquals(List.of(), store.getViolations(rule));
        assertEquals(1, countViolationFiles());
    }

    /** stored.rules plus one file per rule that actually has violations. */
    private long countViolationFiles() throws Exception {
        try (var entries = Files.list(storeDir)) {
            return entries.count();
        }
    }
}
```

Note `countViolationFiles` counts `stored.rules` too, hence the expectations of 1 and 2 rather than
0 and 1 — the assertion messages say what is meant.

- [ ] **Step 2: Run to verify it fails**

Run: `./mvnw -B -pl rules test -Dtest=EmptyOmittingViolationStoreTest`

Expected: FAIL — `cannot find symbol: class EmptyOmittingViolationStore`.

- [ ] **Step 3: Implement the decorator**

```java
package io.github.milczekt1.archrules.freeze;

/**
 * A {@link ViolationStore} that keeps the {@code stored.rules} index complete but writes no file for
 * a rule with zero violations, so a committed freeze store carries no empty files.
 *
 * <p>Register it in {@code archunit.properties}:
 * <pre>{@code freeze.store=io.github.milczekt1.archrules.freeze.EmptyOmittingViolationStore}</pre>
 *
 * <p><strong>The index entry is deliberately kept.</strong> {@code FreezingArchRule} treats a rule
 * the store does not contain as one to seed — it records whatever violations exist and passes. Drop
 * a clean rule's entry and its first real violation would be absorbed as debt instead of failing.
 * Only the (empty) file is removed.
 *
 * <p>Must have a public no-arg constructor: ArchUnit instantiates it reflectively.
 */
public class EmptyOmittingViolationStore implements ViolationStore {

    private final TextFileBasedViolationStore delegate = new TextFileBasedViolationStore();

    private Path storePath;

    @Override
    public void initialize(Properties properties) {
        delegate.initialize(properties);
        storePath = Path.of(properties.getProperty("default.path"));
    }

    @Override
    public boolean contains(ArchRule rule) {
        return delegate.contains(rule);
    }

    @Override
    public void save(ArchRule rule, List<String> violations) {
        delegate.save(rule, violations);
        if (violations.isEmpty()) {
            deleteViolationFile(rule);
        }
    }

    @Override
    public List<String> getViolations(ArchRule rule) {
        return violationFile(rule).filter(Files::exists).isPresent()
                ? delegate.getViolations(rule)
                : List.of();
    }

    private void deleteViolationFile(ArchRule rule) {
        violationFile(rule).ifPresent(file -> {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Could not remove the empty violation file for rule '"
                                + rule.getDescription() + "'", e);
            }
        });
    }

    /**
     * Resolves a rule to the file the delegate stores its violations in.
     *
     * <p><strong>Accepted coupling:</strong> this reads {@code stored.rules}, whose
     * {@code <rule-description>=<uuid>} layout is an implementation detail of
     * {@link TextFileBasedViolationStore}. That class is public, but the file format is not a
     * documented contract, so an ArchUnit upgrade could change it —
     * {@code storedRulesMapsRuleDescriptionToFileName} pins the assumption so such a change fails
     * loudly instead of silently mishandling a consumer's store.
     *
     * @return empty when the rule has no index entry yet
     */
    private Optional<Path> violationFile(ArchRule rule) {
        Path index = storePath.resolve("stored.rules");
        if (!Files.exists(index)) {
            return Optional.empty();
        }
        Properties storedRules = new Properties();
        try (InputStream in = Files.newInputStream(index)) {
            storedRules.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + index, e);
        }
        return Optional.ofNullable(storedRules.getProperty(rule.getDescription()))
                .map(storePath::resolve);
    }
}
```

- [ ] **Step 4: Pin the coupling with a test**

Add to `EmptyOmittingViolationStoreTest`:

```java
    @Test
    void storedRulesMapsRuleDescriptionToFileName() throws Exception {
        // Pins an assumption about TextFileBasedViolationStore's file layout that this decorator
        // depends on. If an ArchUnit upgrade changes it, fail here rather than silently mishandling
        // a consumer's store.
        ArchRule rule = ruleNamed("test.layout-probe");
        store.save(rule, List.of("Class <Foo> is bad"));

        Properties index = new Properties();
        try (var in = Files.newInputStream(storeDir.resolve("stored.rules"))) {
            index.load(in);
        }
        String fileName = index.getProperty("test.layout-probe");

        assertNotNull(fileName, "stored.rules must key violations by rule description");
        assertTrue(Files.exists(storeDir.resolve(fileName)),
                "stored.rules value must name a file in the store directory");
    }
```

- [ ] **Step 5: Run to verify it passes**

Run: `./mvnw -B -pl rules test -Dtest=EmptyOmittingViolationStoreTest`

Expected: PASS — 5 tests, 0 failures.

Then the full module: `./mvnw -B -pl rules test` — green.

- [ ] **Step 6: Commit**

```bash
git add rules/src
git commit -m "feat: add a freeze store that omits empty violation files"
```

---

## Task 4: Wire the store into the example and prove the semantics

**Files:**
- Modify: `rules-example/src/test/resources/archunit.properties`
- Modify: `rules-example/src/test/resources/archunit/frozen/` (re-seeded, committed)

**Interfaces:**
- Consumes: `EmptyOmittingViolationStore` (Task 3).
- Produces: a committed store with no empty files, demonstrating the behaviour to consumers.

> **Freeze-store hygiene, learned the hard way.** Seed, then **commit**, then probe with a new
> violation. Probing first and running `git checkout` afterwards reverts to the last *committed*
> store, discarding the seeding — which is exactly what happened during the previous plan's
> execution. Never commit a store rewritten by a failing run.

- [ ] **Step 1: Register the store**

Add to `rules-example/src/test/resources/archunit.properties`, above the existing
`freeze.store.default.path` line:

```properties
# Keeps stored.rules complete while writing no file for a rule with zero violations, so this
# committed store carries no empty files. The index entry is what keeps a rule frozen — see
# EmptyOmittingViolationStore.
freeze.store=io.github.milczekt1.archrules.freeze.EmptyOmittingViolationStore
```

- [ ] **Step 2: Re-seed the store**

The existing store has an empty file for `test.no-mocked-repository-in-integration-test`. Re-seed so
the new store's layout takes effect:

```bash
./mvnw -B test -pl rules-example -am -Darchunit.freeze.refreeze=true
```

Expected: PASS.

- [ ] **Step 3: Verify the layout**

```bash
ls rules-example/src/test/resources/archunit/frozen/
grep -v '^#' rules-example/src/test/resources/archunit/frozen/stored.rules
```

Expected: `stored.rules` lists **both** rule ids. The directory contains `stored.rules` plus exactly
**one** violation file — the naming rule's, holding the `LegacyChecks` violation. No empty file.

- [ ] **Step 4: Confirm the build is green with no flags**

Run: `./mvnw -B verify`

Expected: PASS.

- [ ] **Step 5: Commit before probing**

```bash
git add rules-example/src/test/resources
git commit -m "docs: use the empty-omitting freeze store in the example consumer"
```

- [ ] **Step 6: Prove a clean rule still fails on its first violation**

This is the invariant the whole design rests on, and it cannot be demonstrated through
`rules-example`: the only rule that ends up file-less there is the mocked-repository rule, and
violating it needs Mockito, which the example deliberately does not have. Adding Mockito purely to
stage a probe would trade away the example's dependency minimalism for a test better placed in the
unit suite.

So assert it in `EmptyOmittingViolationStoreTest` instead:

```java
    @Test
    void aRuleFrozenCleanIsStillContainedSoItsFirstViolationFails() {
        // No file must not mean "unknown rule". FreezingArchRule seeds-and-passes anything the store
        // does not contain, so if this ever returns false a clean rule's first real violation would
        // be absorbed as debt and the build would stay green.
        ArchRule rule = ruleNamed("test.clean-then-dirty");
        store.save(rule, List.of());

        assertTrue(store.contains(rule),
                "a clean rule with no file must still be contained");
        assertEquals(List.of(), store.getViolations(rule));
    }
```

Then confirm end-to-end through `FreezingArchRule` itself, which is the real client:

```java
    @Test
    void freezingArchRuleFailsOnAViolationIntroducedAfterACleanFreeze() {
        ArchConfiguration.get().setProperty("freeze.store.default.path", storeDir.toString());
        ArchConfiguration.get().setProperty("freeze.store.default.allowStoreCreation", "true");
        ArchConfiguration.get().setProperty("freeze.store", EmptyOmittingViolationStore.class.getName());
        try {
            JavaClasses clean = new ClassFileImporter().importClasses(String.class);
            JavaClasses dirty = new ClassFileImporter().importClasses(String.class, Integer.class);

            ArchRule rule = FreezingArchRule.freeze(
                    noClasses().that().haveSimpleName("Integer")
                            .should().haveSimpleName("Integer")
                            .as("test.freeze-roundtrip").allowEmptyShould(true));

            rule.check(clean);   // seeds clean: entry written, no file

            assertThrows(AssertionError.class, () -> rule.check(dirty),
                    "a violation appearing after a clean freeze must FAIL, not be seeded as debt");
        } finally {
            ArchConfiguration.get().reset();
        }
    }
```

`ArchConfiguration` is global process state and Surefire reuses one JVM, so the `finally` reset is
mandatory — leaking `freeze.store` into later test classes would redirect their stores.

If this test does not fail on the second `check`, stop: the decorator is dropping index entries and
the design's central guarantee is broken.

- [ ] **Step 7: Full verification**

Run: `./mvnw -B verify` and `./mvnw -B -pl rules test -Dsurefire.runOrder=reversealphabetical`

Expected: both PASS. `git status --porcelain` shows nothing under `rules-example` — the committed
store must not have been rewritten.

- [ ] **Step 8: Commit any test additions**

```bash
git add rules/src
git commit -m "test: pin that a clean frozen rule still fails on its first violation"
```

---

## Task 5: Untrack the superpowers docs

**Files:**
- Modify: `.gitignore`
- Untrack: `docs/superpowers/` (files stay on disk)

**Interfaces:**
- Consumes: nothing. Independent of every other task; do it last so the spec and plan remain tracked while they are being followed.

- [ ] **Step 1: Untrack and ignore**

```bash
git rm -r --cached docs/superpowers
printf '\n# Superpowers working documents (specs, plans) — kept on disk, not shipped\ndocs/superpowers/\n' >> .gitignore
```

- [ ] **Step 2: Verify files survive on disk**

```bash
ls docs/superpowers/specs docs/superpowers/plans
git ls-files docs/superpowers
```

Expected: the files are listed by `ls`, and `git ls-files` prints **nothing**.

- [ ] **Step 3: Commit**

```bash
git add -A .gitignore docs
git commit -m "chore: untrack superpowers working documents"
```

- [ ] **Step 4: Confirm a clean tree**

```bash
git status --porcelain
```

Expected: only the pre-existing untracked `central-arch-rules-framework-design.patch`.

---

## Final verification

1. `./mvnw -B verify` — reactor green.
2. `./mvnw -B -pl rules test -Dsurefire.runOrder=reversealphabetical` — green.
3. Published rule ids unchanged: exactly `test.class-naming-convention` and `test.no-mocked-repository-in-integration-test`.
4. `rules-example` store: `stored.rules` has both entries; exactly one violation file on disk.
5. `git ls-files docs/superpowers` — empty.
6. Rule classes each hold one rule; `TestingRules` is a wrapper with no rule logic.
7. `git status --porcelain` — only the pre-existing untracked `.patch`.

## Out of scope

- Adding rules, or reviving `DatabaseRules`.
- Reflective derivation of group membership — explicitly rejected in favour of explicit `members()` lists.
- Changes to `RuleDoc`, `RuleRegistry`, `FrozenRules`, `AgentFriendlyFailureDisplayFormat`, or the Lombok wiring.
- The three parked robustness findings from the earlier whole-branch review.
- Publishing, CI, sources/javadoc jars, `Automatic-Module-Name`.
