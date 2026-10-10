# Contributing to Corral

Thanks for considering a contribution. This file covers building the project, the conventions a
change is expected to follow, and — most importantly — [the ways a change to this library can look
green and still be wrong](#what-breaks-consumers).

By participating you agree to the [Code of Conduct](CODE_OF_CONDUCT.md).

## Getting started

Build:

```bash
./mvnw verify
```

The reactor is `corral-sdk` (the framework), `corral-rules` (the rule catalog), one
`corral-rules-<framework><major>` module per framework major that has rules bound to it (see
[Version-bound rules](#version-bound-rules); the root POM's `<modules>` lists them) and
`corral-example` (a working consumer with a committed freeze store, which doubles as an end-to-end
test of the wiring).

**Java baseline.** The root POM's `maven.compiler.release` is the baseline for the whole project. It
is the minimum JVM that can load the published classes, so raising it is a breaking change for
consumers — please do not, without discussing it first. A newer JDK on your machine is fine: `javac`
cross-compiles against its bundled `ct.sym`, so you do not need that exact JDK installed to build.

**Lombok.** Install your IDE's [Lombok](https://projectlombok.org/) plugin, or the IDE reports errors
on generated members that `mvn verify` compiles cleanly.

Lombok is wired through `annotationProcessorPaths` in the root `pom.xml`, not just as a dependency —
JDK 23+ ignores annotation processors that are only on the classpath. Remove that block and
compilation fails on the generated members, so the misconfiguration cannot pass silently.

**Coverage.** JaCoCo fails the build at `verify` below the thresholds set in the root `pom.xml`.
`corral-example` overrides them to zero — it is a wiring demo, not a tested component.

**Sonar.** `corral-example` sets `sonar.skip=true` for the same reason: its classes exist to be
flagged by rules, and its violations are deliberate and committed, so analysing it reports the demo
as findings. It is still compiled and its tests still run — that module is the end-to-end test of
the wiring, and breaking it breaks the build.

## What breaks consumers

Corral's compatibility surface is unusual and sharp. Most of these have **no automated check**, so
they are on the reviewer and on you:

| Change | Why it breaks | Checked by |
|---|---|---|
| Renaming or removing a rule **id** | The id is the freeze-store key. Consumers' recorded violations are filed under the old id, so the rule silently stops enforcing — a green build with zero enforcement. It also silences any consumer excluding that id: the exclusion now names nothing, which logs a warning rather than failing the build. | the catalog test's expected id set — the change shows as a diff in review |
| Changing a rule's **predicate text** | Predicate text is also a freeze-store matching key. On upgrade, old violations resurface and the consumer's build fails on code they did not touch. | nothing |
| Raising the **Java baseline** | It is the minimum JVM that can load the published classes. | nothing |
| Testing against the published frozen field instead of the raw `DEFINITION` | The frozen field seeds and passes, so the test is vacuous. | partially |
| Letting the **[rules catalog](docs/rules.md)** drift from the published groups | The most visible form of catalog rot. | `RulesCatalogDocTest` — ids and groups in the table must match what `EveryPublishedGroup` publishes |

If you must rename an id or reword a predicate, say so explicitly in the PR and describe the
migration for existing consumers.

## The shape

One rule, one class. A rule class under `rules/<topic>/` owns everything about that rule; a group
under `groups/` is a thin wrapper composing rule classes. Class names end in `Rule`, so they never
collide with the `*Test` convention their own tests follow. A group of groups is legal and nests to
any depth, but the catalog does not publish one: composing the groups a build runs is the consumer's
call, made in their repo. `EveryPublishedGroup`, in this module's **test** sources, is that root for
Corral's own tests only.

Membership is declared once, as `@ArchTest ArchTests` fields. `ArchTests.in(X)` descends into
exactly those fields and nothing else, so the field *is* the membership — there is no second list to
keep in step, and no way to declare a member that consumers never evaluate.

Packages split by role and the arrows point one way, enforced by the module boundary rather than by
convention: `corral-sdk` has no dependency on `corral-rules`, so a framework class importing a
concrete rule does not compile.

The step-by-step for adding a rule is **[Creating a rule](docs/creating-a-rule.md)** — id, class,
fixtures, wiring, and the four ways to ship a rule that silently enforces nothing.

## A rule in more than one group

Membership is a graph, not a tree: nothing stops two groups naming the same rule class, and over
time a rule genuinely belonging to two axes (say testing *and* security) will.

That works, and it costs a little. ArchUnit builds one JUnit node per `@ArchTest` field it reaches,
so the rule appears once under each group and its predicates re-run over the same classes. Class
import happens once — `JavaClasses` is cached per `@AnalyzeClasses` — and the freeze store holds one
entry, because both nodes share the description and so cannot disagree. Nothing deduplicates the
nodes; collapsing them would mean owning node creation, which is not worth it for a repeated
predicate pass over already-imported classes.

The catalog test allows this on purpose. It groups published rules by id and requires **one distinct
rule object** per id — a rule reached by two paths is one object read from one `static final` field,
while two rules colliding on an id are two. That collision is the real hazard, because the id is the
freeze-store key, so the two would read each other's recorded violations as their own. `RuleRegistry`
does not catch it: its guard compares docs, so two rules carrying identical documentation pass
straight through.

Default to one group per rule and compose by nesting groups. Reach for a second parent when the rule
really does belong under both.

## Adding a group

On top of the rule steps:

1. Create the group class under `groups/` — a `@UtilityClass` with one `@ArchTest ArchTests` field
   per member. Any existing group is a working template.
2. Give it an `@ArchTest ArchTests` field on `EveryPublishedGroup` (test sources). Without one, no
   test here walks the group, so its rules skip the doc, grammar and id-uniqueness checks — the
   catalog test's reachability check fails until you add it.
3. Add it to the [rules catalog](docs/rules.md) so consumers know it exists. Nothing delivers a new
   group automatically: it reaches a build when someone wires it, which is the point.

## Version-bound rules

A rule is **version-bound** when it only makes sense on one major of a framework: below some
version a violation has nothing to migrate to, or from the next major on the API it bans is gone and
the rule can never fire. A rule about a deprecated API is the usual case. Such a rule ships in a
module for that major, never in `corral-rules`, for two reasons:

- **Testing.** One `groupId:artifactId` sits on a classpath at one version only, so fixtures built
  from one major's real jars cannot share `corral-rules`' test classpath with another major's.
- **Dropping it.** The version is in the artifact id, so on the next major the consumer edits a POM
  that names what to remove. The version scope is marked, not enforced: nothing checks the
  consumer's framework version at run time.

A rule that holds on every version of a framework is not version-bound and stays in `corral-rules`,
whatever it matches.

### Names

| | Shape | Example |
|---|---|---|
| Artifact id | `corral-rules-<framework><major>`, framework name hyphenated as its own artifacts spell it | `corral-rules-spring-boot3`, `corral-rules-jackson2` |
| Package root and `Automatic-Module-Name` | `io.github.milczekt1.corral.<framework><major>`, no hyphens | `io.github.milczekt1.corral.springboot3` |
| Group | `<Framework><major><Topic>RulesGroup` in `<root>.groups`, so the consumer's wiring line carries the version | `SpringBoot3DeprecationsRulesGroup` |
| Rules | `<root>.rules.<topic>.<rule>`, as in `corral-rules` | |
| `docs/rules.md` heading | `## <Framework> <major> rules`, then the artifact id in backticks inside parentheses — `RulesCatalogDocTest` finds the section by it | ## Spring Boot 3 rules (`corral-rules-spring-boot3`) |

Never reuse `io.github.milczekt1.corral.groups` or `.rules` in a version module: a package split
across two jars breaks JPMS automatic modules. Rule ids follow the ordinary grammar and carry no
version; they are never renamed, in a version module as anywhere else.

### Adding a rule to an existing version module

The ordinary rule steps ([Creating a rule](docs/creating-a-rule.md)), with every path under the
module instead of `corral-rules`, the store seeded with `-pl <module>`, and the id added to both the
module's own group test and `PublishedCatalogTest.ruleDiscoveryDescendsThroughNestedGroups`.

### Adding a version module

`corral-rules-spring-boot3` is the worked example; copy its shape.

1. **POM.** Copy `corral-rules-spring-boot3/pom.xml`. Set the artifact id, the `<name>`, and a
   `<description>` stating the version window and that the dependency goes on the next major's
   upgrade — IDEs and Maven Central show it. Set the `Automatic-Module-Name`. Keep the Surefire
   `**/fixtures/**` exclude.
2. **Dependencies.** Compile scope is `corral-sdk` alone, Lombok `provided`. The framework's real
   jars go in **test** scope with their transitives intact — rules match by name, so main code never
   needs them. Where a version the root POM manages (such as `spring.version`) would override the
   framework's own transitives, override that property in the module POM. Check both:
   `./mvnw -pl <module> dependency:tree -Dscope=runtime` lists only `corral-sdk` and its
   transitives, and the full `dependency:tree` holds no jar from another major.
3. **Reactor.** Add the module to the root POM's `<modules>`. Publishing needs nothing more: the
   release workflow finds every module from its POM.
4. **Group.** One group per module to start, named as above. Its Javadoc states the version window
   and the three things to remove on the next major: the dependency, the field wiring the group,
   and the `archunit/frozen` entries for its rule ids.
5. **Group test.** A test in the module asserting the exact ids the group publishes. Without it
   the coverage gate (`missed classes = 0`) fails, because nothing else in the module loads the
   group.
6. **Catalog tests in `corral-rules`.** They walk every published group, version modules included,
   and find each module's groups and its `docs/rules.md` section by convention — no list of modules
   to extend. They need two things:
   - a **test-scope** dependency from `corral-rules` on the module — its main jar carries no
     framework, so nothing clashes. Without it the module is invisible to every catalog check;
   - an `@ArchTest ArchTests` field for the group on `EveryPublishedGroup`.
7. **Catalog page.** A `docs/rules.md` section under the heading above, before *Standalone rules*:
   the version window in prose, a dependency snippet whose XML comment says what to remove on the
   next major (consumers copy the comment into their POM with it), the wiring line, and the table.
   The Spring Boot 3 section is the template.

When a framework's next major gets version-bound rules of its own, they go in a new module
(`corral-rules-spring-boot4`), never into the previous major's.

## Is a rule catalog-worthy?

Not every good rule belongs here. A rule earns a place in the central catalog when it is:

- **Broadly applicable** — true for most JVM projects, not a house style.
- **Objectively checkable** — no judgement call about intent.
- **Stable** — the predicate is unlikely to need rewording, because rewording it is a breaking
  change for every consumer.
- **Non-obvious to fix** — the HOW TO FIX says something the reader would not have done unprompted.
  Twelve agent runs against `no-system-out` and `no-thread-sleep` fixed both correctly with or
  without the guidance ([evidence](docs/evidence.md)); a rule earns its failure message where that
  is not true.

A rule that encodes one team's preference is better off in that team's own rule namespace. The SDK
exists so you can author those without forking anything.

## Rule ids

An id is the freeze-store key (see [What breaks consumers](#what-breaks-consumers)), so its grammar
is closed and the closure is enforced, not a style convention.

**Every id Corral publishes starts with the `corral.` vendor prefix.** Corral can enforce its own ids
against its own catalog test, but it deliberately cannot enforce a consumer's — `RuleDoc` is public
SDK surface, narrowed to universal hygiene precisely so a consumer keeps their own namespace. That
freedom means a consumer authoring rules with `corral-sdk` reaches for the same generic words
Corral's own catalog used to claim — `test`, `logging`, `api`, `security`, `naming`. Before the
prefix, a collision there was prevented only by a runtime `RuleRegistry.register` throw that forced
the *consumer* to rename *their* rule. Taking `corral.` for every id this library publishes makes the
collision structurally impossible instead, and leaves the entire generic namespace to consumers. A
consumer's own rule needs no prefix at all — `corral-example`'s `acme.no-stdout-in-services` is
exactly that case, and it must keep working.

| Shape | When | Example |
|---|---|---|
| `corral.<slug>` | Corral's own framework meta-check | `corral.no-unregistered-rule` |
| `corral.<concern>.<slug>` | a catalog rule | `corral.logging.no-system-out` |
| `corral.<concern>.<library>.<slug>` | a catalog rule specific to one library | `corral.test.mockito.no-static-mocking` |

**Segment 1 is always `corral`** — provenance, not taxonomy. **Segment 2 is a closed vocabulary**, in
four kinds: a **concern** (`logging`, `security`, …), a **library** (`spring`, `jackson`, …), the
**JDK** (`java`, plus `java<N>` for a version-gated API), or **meta** — the one exception, where a
depth-2 `corral.<slug>` id has no segment-2 concern at all and the slug itself sits at segment 2.
**Segment 3, when present, is a library qualifier.** The accepted words for each are the lists in
`RuleIdGrammarTest`; that test is the vocabulary, so adding a word means adding it there and nowhere
else.

**Tie-break when a predicate touches a library:** the segment-3 qualifier is the non-JDK library whose
*correct use* the rule asserts, not any library the predicate merely detects. A rule that flagged test
libraries leaking into production code, for instance, would name JUnit and Mockito only to detect
them — it polices layering, not correct use of either library — so it would stay `corral.layering.*`
rather than take on either library's qualifier.

**Exactly two polarity markers.** A slug either starts with `no-` (a prohibition) or contains
`-must-`, read as `<subject>-must-<predicate>` (`fields-must-be-final`: the subject is fields, the
predicate is being final). Six inconsistent forms across the early catalog collapsed into these two
so a slug's intent is legible without opening the rule.

**Caps:** depth ≤ 4 segments, and a fourth segment is legal only when segment 3 is a library
qualifier. Anything finer-grained belongs in a group, which can be reorganised, not in the id, which
cannot once a consumer has frozen it. Length ≤ 72 characters — segment 1 is a fixed vendor prefix on
top, so the taxonomy budget below it is unchanged from before the prefix existed.

**The check is split across two layers, deliberately.** `RuleDoc`'s constructor enforces only the
shape regex, the length cap and the depth cap — universal hygiene that binds every id, including a
consumer's own, because the id becomes a *file name* in every consumer's freeze store (see [The
freeze store](docs/configuration.md#the-freeze-store)). The vendor prefix, the segment vocabularies
and the polarity marker are enforced separately, by `RuleIdGrammarTest` in `corral-rules`, and only
against ids reachable from `EveryPublishedGroup` — this catalog, not the world. `corral-sdk` is
published precisely so a consumer can author rules in their own namespace ("[a rule that encodes one
team's preference is better off in that team's own rule namespace](#is-a-rule-catalog-worthy)"); a
closed vocabulary enforced inside `RuleDoc` itself would revoke that promise.

**Deprecate, never rename.** A rule id is a freeze-store key, and ArchUnit's `ViolationStore` SPI has
no rename verb — so a rename orphans every consumer's recorded violations, silently. Withdraw an id
with `DeprecatedRule.supersededBy(...)` instead: **[Retiring a rule](docs/retiring-a-rule.md)**.

## Commit conventions

Conventional Commits, capitalised subject, imperative mood:

```
feat: Add SpringRules with spring.no-field-injection
fix: Resolve Sonar findings and add SonarCloud badges
docs: Split the contributing guide out of the README
build: Target Java 17 across the whole project
```

Every change lands on `main` through a squash-merged pull request, so the PR title becomes the
commit message. Write the title accordingly.

## Pull requests

- `./mvnw clean verify` passes locally before you open it.
- Fill in the PR template, including the rule-change checklist if you touched a rule.
- `main` requires linear history, an up-to-date branch, and all conversations resolved.
- Sonar runs on PRs from branches in this repository. On PRs from forks it is skipped, because the
  token is not exposed to forks — that is expected and does not block the merge.

## Reporting problems

Bugs and rule proposals go in [issues](https://github.com/MilczekT1/Corral/issues).

**Security vulnerabilities do not** — see [SECURITY.md](SECURITY.md).
