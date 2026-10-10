# Release process

A release is two workflows. **Release** (manual) prepares a PR; **Publish release** runs when that
PR's commits land on `main`, uploads to **Maven Central**, and tags. Only allowlisted users may trigger either by hand.

1. Go to **Actions → Release → Run workflow**.
2. Enter:
    - **releaseVersion** — the version to release, e.g. `0.1.0` (no `-SNAPSHOT`).
    - **nextVersion** — the next development version, e.g. `0.1.1` (no `-SNAPSHOT`; the
      workflow appends it).
3. Run it. It checks you are on the allowlist and that neither the tag nor the branch exists,
   then pushes `release/v<version>` with two commits — `Release Corral <version>` (crediting you
   as co-author) and the bump to `<nextVersion>-SNAPSHOT` — opens a PR to `main` and enables
   **auto-merge**. The PR rebase-merges once the required checks pass.
4. The merge pushes both commits to `main`, and the push runs **Publish release**. It finds the `Release Corral <version>` commit **as it
   landed on `main`**, checks its project version, runs `./mvnw clean verify` on it, then signs and
   uploads `corral-sdk`, `corral-rules` and `corral-parent` — consumers need the parent pom to
   resolve the managed dependency versions — tags that commit `v<version>`, and creates the GitHub
   Release.
5. **Press Publish.** The upload stops at *VALIDATED*. Open
   [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments),
   inspect the deployment, and press **Publish**. It reaches `repo1.maven.org` within about
   30 minutes; search indexes lag further. Pressing **Drop** instead discards it; re-upload by
   dispatching **Publish release** with the version (below).

Tagging and uploading wait for the merge because GitHub's rebase merge re-creates every commit with
a new hash: a tag made on the release branch would point at a commit that is not on `main`. `v0.1.0`
predates this and points at `c13cf97`, whose tree is identical to `30ed93f` on `main`.

Merge release PRs with **rebase**, never squash: a squash folds the release commit into the snapshot
bump, and **Publish release** either never starts or fails because it cannot find a commit at the
release version.

**If Publish release fails**, fix the cause and re-run its failed job, or dispatch **Actions →
Publish release** from `main` with the version. Every step skips what is already done — the tag if it is
on the release commit, the upload if `repo1.maven.org` already serves the version, the GitHub
Release if it exists — and it refuses to continue if `v<version>` points anywhere else.

The release tree is built on its own before uploading: the release PR's checks ran on the snapshot
bump above it, and a version published to Central can never be deleted or replaced. The `deploy`
step itself then uses `-DskipTests=true` rather than testing twice.

`corral-example` is never published — `skipPublishing` is set on its
`central-publishing-maven-plugin`, and the already-published check in **Publish release** reads the
same flag.

The trigger allowlist is hardcoded in `.github/workflows/release.yml`,
`.github/workflows/publish-release.yml` and `.github/workflows/publish-java.yml` as
`RELEASE_ALLOWED_ACTORS` (space-separated GitHub usernames); edit all three via a normal PR.

## Snapshots

Snapshots are published manually: dispatch **Publish artifact** on `main`, the only ref it can
run from. They go to Central's snapshot repository, publish
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

## Signing and credentials

Signing and upload happen only under the `central` Maven profile, which only the workflows
activate, so a local `./mvnw install` needs neither a key nor a token. The profile signs with
BouncyCastle (`<signer>bc</signer>`), reading the key from the environment — no `gpg` binary.

| Secret | Environment | Contents |
|---|---|---|
| `CENTRAL_TOKEN_USERNAME` | `maven-central` | Central Portal user token — username part |
| `CENTRAL_TOKEN_PASSWORD` | `maven-central` | Central Portal user token — password part |
| `GPG_PRIVATE_KEY` | `maven-central` | ASCII-armored private signing key (`gpg --armor --export-secret-keys <id>`) |
| `GPG_PASSPHRASE` | `maven-central` | Its passphrase |
| `RELEASE_APP_ID` | `release` | Numeric App ID of the GitHub App that opens the release PR |
| `RELEASE_APP_PRIVATE_KEY` | `release` | That App's private key (`.pem`) |

Both environments allow deployments from `main` only, with administrator bypass off, so a
workflow on any other branch — or a `pull_request` run, whose ref is `refs/pull/N/merge` — gets
none of these. Keep them out of repository secrets: those reach every workflow run on every pushed
branch. **Publish release** is triggered by push to `main` rather than by the merged PR for the same
reason.

The release PR is opened with the App's token, not `GITHUB_TOKEN`: a PR opened by `GITHUB_TOKEN`
triggers no workflows, so its required checks never run and auto-merge waits forever. The App needs
**Contents** and **Pull requests** read/write, no webhook, and must be installed on this repository.

One-time setup, outside the repository:

1. Sign in to [central.sonatype.com](https://central.sonatype.com) with the GitHub account that
   owns the repository; the `io.github.milczekt1` namespace verifies from that. Enable
   **snapshots** for the namespace.
2. **Account → Generate User Token**; store both halves as the secrets above, in the
   `maven-central` environment.
3. Generate a signing key and publish its public half, which Central checks every signature
   against:
   ```bash
   gpg --quick-gen-key "Konrad Boniecki <konrad_boniecki@hotmail.com>" rsa4096 sign 2y
   gpg --keyserver keyserver.ubuntu.com --send-keys <fingerprint>
   gpg --keyserver keys.openpgp.org --send-keys <fingerprint>
   ```
   Central looks keys up by fingerprint, so neither needs email verification. keys.openpgp.org
   drops the user id until verified; request that at keys.openpgp.org/manage if wanted.

A user token or signing key that expires fails **Publish release** at upload, after the release PR
has merged but before anything is tagged; rotate the secret and re-run the failed job.
