# Release process

Releases run in CI via the **Release** GitHub Actions workflow (manual `workflow_dispatch`
trigger) and go to **Maven Central**. Only allowlisted users may trigger it.

1. Go to **Actions → Release → Run workflow**.
2. Enter:
    - **releaseVersion** — the version to release, e.g. `0.1.0` (no `-SNAPSHOT`).
    - **nextVersion** — the next development version, e.g. `0.1.1` (no `-SNAPSHOT`; the
      workflow appends it).
3. Run it. The workflow checks you are on the allowlist, creates branch `release/v<version>`,
   sets the release version and commits + tags `v<version>` on it (crediting you as
   co-author), signs and uploads `corral-sdk`, `corral-rules` and `corral-parent` to the
   Central Portal — consumers need the parent pom to resolve the managed dependency
   versions — bumps to `<nextVersion>-SNAPSHOT`, pushes the branch + tag, creates the GitHub
   Release, then opens a PR (`release/v<version>` → `main`) and enables **auto-merge**. The PR
   merges automatically once the required build check passes.
4. **Press Publish.** The upload stops at *VALIDATED*. Open
   [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments),
   inspect the deployment, and press **Publish**. It reaches `repo1.maven.org` within about
   30 minutes; search indexes lag further. Pressing **Drop** instead discards it, but the tag and
   GitHub Release already exist — re-upload from the tag with **Publish artifact** (below).

Before uploading, the workflow runs `./mvnw clean verify` on the version-bumped tree. That tree
has never been built by any CI run — `versions:set` has just rewritten the poms — and a version
published to Central can never be deleted or replaced, so the tests and the coverage gate run
against exactly what is about to be released. The `deploy` step itself then uses
`-DskipTests=true` rather than testing twice.

`corral-example` is never published — `skipPublishing` is set on its
`central-publishing-maven-plugin`, and the workflow's already-published check reads the same flag.

The trigger allowlist is hardcoded in `.github/workflows/release.yml` as
`RELEASE_ALLOWED_ACTORS` (space-separated GitHub usernames); edit it via a normal PR.

## Snapshots

Snapshots are published manually: dispatch **Publish artifact** on a ref whose project version
ends in `-SNAPSHOT` (`main`, typically). They go to Central's snapshot repository, publish
immediately with no validation, can be re-deployed as often as needed, and are deleted after 90
days. A consumer needs this repository to resolve
them — no credentials:

```xml
<repository>
  <id>central-snapshots</id>
  <url>https://central.sonatype.com/repository/maven-snapshots/</url>
  <releases><enabled>false</enabled></releases>
  <snapshots><enabled>true</enabled></snapshots>
</repository>
```

**Publish artifact** on a release tag re-uploads that version, which only works if it has not
been published yet.

## Signing and credentials

Signing and upload happen only under the `central` Maven profile, which only the workflows
activate, so a local `./mvnw install` needs neither a key nor a token. The profile signs with
BouncyCastle (`<signer>bc</signer>`), reading the key from the environment — no `gpg` binary.

| Repository secret | Contents |
|---|---|
| `CENTRAL_TOKEN_USERNAME` | Central Portal user token — username part |
| `CENTRAL_TOKEN_PASSWORD` | Central Portal user token — password part |
| `GPG_PRIVATE_KEY` | ASCII-armored private signing key (`gpg --armor --export-secret-keys <id>`) |
| `GPG_PASSPHRASE` | Its passphrase |

One-time setup, outside the repository:

1. Sign in to [central.sonatype.com](https://central.sonatype.com) with the GitHub account that
   owns the repository; the `io.github.milczekt1` namespace verifies from that. Enable
   **snapshots** for the namespace.
2. **Account → Generate User Token**; store both halves as the secrets above.
3. Generate a signing key and publish its public half, which Central checks every signature
   against:
   ```bash
   gpg --quick-gen-key "Konrad Boniecki <konrad_boniecki@hotmail.com>" rsa4096 sign 2y
   gpg --keyserver keyserver.ubuntu.com --send-keys <fingerprint>
   gpg --keyserver keys.openpgp.org --send-keys <fingerprint>
   ```
   Central looks keys up by fingerprint, so neither needs email verification. keys.openpgp.org
   drops the user id until verified; request that at keys.openpgp.org/manage if wanted.

A user token or signing key that expires fails the next release at upload, after the tag is
created locally but before anything is pushed.
