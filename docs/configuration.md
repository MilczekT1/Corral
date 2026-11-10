# Configuration reference

Everything a consumer can set, in `src/test/resources/archunit.properties` unless noted otherwise.
The two lines the [quick start](../README.md#quick-start) asks for are the first two rows.

| Property | Required | Notes |
|---|---|---|
| `freeze.store.default.path` | yes | Resolved against the JVM's **working directory**, not the classpath. Maven sets it to the module directory; an IDE run configuration with a different working directory will not find the store. |
| `failureDisplayFormat` | recommended | Without it you get ArchUnit's default one-line output instead of WHY / HOW TO FIX. |
| `freeze.store` | optional | Set to `io.github.milczekt1.corral.store.EmptyOmittingViolationStore` to keep empty violation files out of your commits — see below. |
| `freeze.store.default.allowStoreCreation` | **never commit as `true`** | See the warning below. |
| `freeze.store.default.allowStoreUpdate` | `false` in CI | Passed on the CI command line, not committed. See [CI](#ci). |
| `corral.exclude.<rule-id>` | optional | In `corral.properties`, not `archunit.properties`. Removes the named rule from your build permanently; the value is the mandatory reason. See [Excluding a rule](excluding-a-rule.md). |
| `corral.ignore-patterns.fail` | optional, defaults to `true` | In `corral.properties`, not `archunit.properties`; there is no system-property override. Every Corral rule fails, naming each copy found, when `archunit_ignore_patterns.txt` is on the classpath. Set it to `false` if that file is yours and deliberate. |
| `corral.generated-sources` | optional | In `corral.properties`. Comma-separated directories, relative to the module root (`../` allowed), holding generated sources outside `target/`, `build/` and `out/`. Classes compiled from them are not checked. See [Generated code](#generated-code). |

> **Do not put `freeze.store.default.allowStoreCreation=true` in `archunit.properties`.** ArchUnit
> defaults it to `false`, and that default is the only thing separating "the store is missing" from
> "silently freeze everything and pass". Pinned to `true`, a store that was never committed, got
> gitignored, or was lost to a shallow checkout gives you a green build with every violation
> re-frozen and no signal at all. Left at its default, the same situation fails loudly with
> `Creating new violation store is disabled (…)`. ArchUnit merges any `archunit.`-prefixed system
> property, which is why the one-off override that seeds the store works without editing the
> committed file.

## CI

`allowStoreCreation` guards only the creation of `stored.rules`. A rule missing from an existing
`stored.rules` is recorded under `freeze.store.default.allowStoreUpdate`, which ArchUnit defaults to
`true`, so it seeds and passes ([How freezing decides](freezing.md)). If its entry is never committed,
every CI run records whatever violations its checkout holds, including those the change under build
introduced. An upgrade that adds a rule to a group you wire reaches CI that way.

Forbid store writes in CI:

```bash
mvn verify -Darchunit.freeze.store.default.allowStoreUpdate=false
```

Under Gradle, `-D` on the command line does not reach the test JVM: set it with `systemProperty` in
the `test` task, or the flag is ignored and the gap stays open.

Any store write then fails the build. Run the build locally and commit the store change. Do not
follow ArchUnit's message, `Updating frozen violations is disabled (enable by configuration
freeze.store.default.allowStoreUpdate=true)`: enabling it in CI reopens the gap. Corral's failure
format cannot reword it, because it is an exception rather than a rule failure.

Two kinds of change then need a local build and a store commit before they merge:

- **A dependency bump that adds rules**, such as Dependabot raising Corral. It cannot merge unattended.
- **A fix for recorded debt.** ArchUnit drops solved violations from the store. Left uncommitted, CI's
  checkout keeps the fixed violation recorded, and reintroducing it passes.

### Failing on a changed store instead

To fail with your own message, let the build write and then check that it did not. Both consequences
above still apply.

```yaml
- name: Freeze stores unchanged by the build
  run: |
    changed=$(git status --porcelain -- ':(glob)**/src/test/resources/archunit/frozen/**')
    if [ -n "$changed" ]; then
      echo "::error::The build changed the freeze store. Run the build locally and commit the store changes:"
      echo "$changed"
      exit 1
    fi
```

It sees a modified `stored.rules` and a new, untracked violation file. Each of these makes it pass
without checking anything:

- **A simpler pathspec.** `'*/src/test/resources/archunit/frozen'` matches nothing: git compares a
  wildcard pathspec against each full file path, and no file path ends in `frozen`. The `:(glob)`
  form covers single- and multi-module layouts.
- **Another store path.** Replace `src/test/resources/archunit/frozen` with your
  `freeze.store.default.path`.
- **A gitignored store.** `git status` does not list ignored files.
- **Another job.** Run it in the job that ran the build, after the build.

## `corral.properties`

Corral's own settings live in `src/test/resources/corral.properties`, a file only the consuming
project owns. Libraries, including Corral's own jars, never ship one.

```properties
corral.base-package=com.acme.billing
corral.property-prefix=acme
corral.ignore-patterns.fail=true
corral.exclude.corral.logging.no-system-err=We ship a CLI; stderr is the interface. ADR-021.
```

| Key | Notes |
|---|---|
| `corral.base-package` | The project's root package, for rules that declare they need it. Must be a valid Java package name. A rule that declares it fails, naming the key, when it is missing or when no imported class resides in it. |
| `corral.property-prefix` | The one namespace the project's own `@ConfigurationProperties` keys live under, for rules that declare they need it. Must be a Spring Boot property name: dot-separated elements of lower-case letters, digits and `-`, none starting with `-`. Choose it once per project, and choose a value no Kubernetes Service in the namespace is named after or starts with followed by `-`: a Service `acme-redis` injects `ACME_REDIS_PORT`, which binds to `acme.redis.port`. A wrong but valid value is not loud: seeding the store records every existing site as debt, and the build fails only on the next class that uses the right prefix. |
| `corral.ignore-patterns.fail` | See the table above. |
| `corral.exclude.<rule-id>` | See [Excluding a rule](excluding-a-rule.md). |
| anything outside `corral.` | Read by your own rules. A key no rule in the run declares logs a warning. |

- Read as UTF-8. A duplicate key, an unknown `corral.*` key or an invalid value fails **every** rule,
  naming the file and each problem: a file that is not understood is not trusted.
- When several copies are on the classpath, the first one wins and a warning names them all. Maven,
  Gradle and IntelliJ put the module's own test resources first. A copy read from inside a jar
  always produces a warning.

## The freeze store

`freeze.store=io.github.milczekt1.corral.store.EmptyOmittingViolationStore` changes two things
about how the store is written.

**Violation files are named after the rule id.** Stock ArchUnit names them with a random UUID, so
reading a store means resolving names through `stored.rules` first:

```text
archunit/frozen/
├── stored.rules
├── corral.test.class-names-must-end-with-test-or-it  # instead of 56d55a4e-91ac-4e12-8682-030d6f3f746f
└── acme.no-stdout-in-services
```

`git log -p archunit/frozen/corral.test.class-names-must-end-with-test-or-it` is then that rule's debt history. Only ids
get this treatment: the store is global, so it also serves rules frozen without a `RuleDoc`, whose
descriptions are whole sentences — those keep a UUID. Names are assigned once, when a rule is first
frozen, so existing stores keep their UUIDs and keep working.

**A clean rule leaves no file.** A rule that is already clean still gets frozen: ArchUnit records it
in `stored.rules` *and* writes an empty violation file. The index entry is what keeps the rule
enforced (see [How freezing decides](freezing.md)); the empty file is noise in
a commit. This store keeps the entry and drops the file. Three things to know:

- **Opt-in.** Leave the line out and you get ArchUnit's stock store, empty files included.
- **Global per run.** ArchUnit uses it for *every* `FreezingArchRule` in the run, including your own.
  The behaviour change is narrow and the index entry is always preserved, so your own frozen rules
  stay exactly as enforced.
- **Not retroactive.** Switching it on leaves already-committed empty files in place —
  `FreezingArchRule` only writes when it has something to change, and a rule that is clean and stays
  clean never triggers a write. Clear them once with `mvn test -Darchunit.freeze.refreeze=true` (or
  delete them by hand) and review the diff: `refreeze` re-records *every* rule, so anything currently
  failing gets absorbed as debt too.

## Multi-module builds

`@AnalyzeClasses(packages = "com.acme", importOptions = ImportOption.DoNotIncludeJars.class)` scans
whatever the classpath holds under that package, and the classpath depends on the command. For a
module `app` that depends on `core` in the same build:

| Run | `core` on `app`'s classpath as | `app` itself as |
|---|---|---|
| `mvn test`, the IDE | `core/target/classes` | `app/target/classes` |
| `mvn verify`, `mvn install` | `core-<version>.jar` | `app/target/classes` |
| `mvn test -pl app` | `core-<version>.jar` from `~/.m2` | `app/target/classes` |
| Failsafe `*IT` tests under `mvn verify` | `core-<version>.jar` | `app-<version>.jar` |

`DoNotIncludeJars` drops only the jars. So `app`'s scan includes `core` under `test` and in the IDE
but not under `verify`; the freeze store a module records changes with the command that seeded it,
and one violation in `core` fails in every module downstream of it. An architecture test run by
Failsafe scans none of its own module's production classes, and its production rules pass with
nothing to check.

`CurrentModuleLocations` scans the module the test class was compiled in — its test and production
output — and nothing else, however the test is launched:

```java
@AnalyzeClasses(locations = CurrentModuleLocations.class)
```

- Each module's architecture test checks that module. A module without one is not checked.
- Do not add `packages` or `packagesOf` next to it: `@AnalyzeClasses` imports them as well, and the
  sibling modules come back.
- It recognises Maven `target/test-classes`, Gradle `build/classes/<lang>/test` and IntelliJ
  `out/test/<module>`, and fails the test for any other layout. A Gradle `integrationTest` or
  `testFixtures` source set needs your own `com.tngtech.archunit.junit.LocationProvider`.
- Switching an existing build to it narrows each module's scan to its own classes, so on the next
  run the store drops the sibling modules' violations it had recorded. Review that diff once.

## Generated code

Every Corral rule checks only the code you maintain. A class is skipped when the source file it was
compiled from is only under the module's `target/`, `build/` or `out/` directory, or in a directory
listed in `corral.generated-sources`. Every other class is checked, including one whose source file
Corral cannot find. A package named `build` deeper down is unaffected. That covers the output of
OpenAPI, protobuf, jOOQ, MapStruct and other generators in their default locations, with no
configuration. The first rule that skips anything prints how many classes it skipped.

- **Generated code committed to the module**, anywhere outside those three directories, cannot be
  told apart from code you maintain. List its directories, relative to the module root; `../` reaches generated sources
  another module or a shared folder holds:

  ```properties
  corral.generated-sources=src/main/java/com/acme/jooq,\
    ../proto/build/generated
  ```

  A directory holding maintained sources is refused, because it would skip everything: `src`,
  `src/<set>`, and `src/<set>/<language>` for `main`, `test` and any `*Test` set, also below
  `../<module>/` and wherever the path resolves. A generator's own root such as `src/gen/java`, and
  any directory below a source root, is accepted. Every listed directory is printed on every failure, as exclusions are.
- **Members generated inside your own classes** — Lombok getters, record accessors, Kotlin
  `data class` methods — are checked. They come from declarations you maintain.
- **Classes from a jar**, and classes from an output layout Corral does not recognise, are checked.
- **A class compiled without debug info** (`-g:none`) records no source file. Corral looks for a file
  named after its top-level class and skips the class when none exists, so a maintained class
  compiled that way from sources outside the module, or a second top-level class sharing another
  class's file, is not checked.
- Upgrading drops the violations a store had recorded in generated code. Commit that diff.
