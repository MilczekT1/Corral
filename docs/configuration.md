# Configuration reference

Everything a consumer can set, in `src/test/resources/archunit.properties` unless noted otherwise.
The two lines the [quick start](../README.md#quick-start) asks for are the first two rows.

| Property | Required | Notes |
|---|---|---|
| `freeze.store.default.path` | yes | Resolved against the JVM's **working directory**, not the classpath. Maven sets it to the module directory; an IDE run configuration with a different working directory will not find the store. |
| `failureDisplayFormat` | recommended | Without it you get ArchUnit's default one-line output instead of WHY / HOW TO FIX. |
| `freeze.store` | optional | Set to `io.github.milczekt1.corral.store.EmptyOmittingViolationStore` to keep empty violation files out of your commits — see below. |
| `freeze.store.default.allowStoreCreation` | **never commit as `true`** | See the warning below. |
| `corral.exclude.<rule-id>` | optional | In `corral.properties`, not `archunit.properties`. Removes the named rule from your build permanently; the value is the mandatory reason. See [Excluding a rule](excluding-a-rule.md). |
| `corral.ignore-patterns.fail` | optional, defaults to `true` | In `corral.properties`, not `archunit.properties`; there is no system-property override. Every Corral rule fails, naming each copy found, when `archunit_ignore_patterns.txt` is on the classpath. Set it to `false` if that file is yours and deliberate. |

> **Do not put `freeze.store.default.allowStoreCreation=true` in `archunit.properties`.** ArchUnit
> defaults it to `false`, and that default is the only thing separating "the store is missing" from
> "silently freeze everything and pass". Pinned to `true`, a store that was never committed, got
> gitignored, or was lost to a shallow checkout gives you a green build with every violation
> re-frozen and no signal at all. Left at its default, the same situation fails loudly with
> `Creating new violation store is disabled (…)`. ArchUnit merges any `archunit.`-prefixed system
> property, which is why the one-off override that seeds the store works without editing the
> committed file.

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
