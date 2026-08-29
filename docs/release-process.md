# Release process

Releases run in CI via the **Release** GitHub Actions workflow (manual `workflow_dispatch`
trigger). Only allowlisted users may trigger it.

1. Go to **Actions → Release → Run workflow**.
2. Enter:
    - **releaseVersion** — the version to release, e.g. `0.1.0` (no `-SNAPSHOT`).
    - **nextVersion** — the next development version, e.g. `0.1.1` (no `-SNAPSHOT`; the
      workflow appends it).
3. Run it. The workflow checks you are on the allowlist, creates branch `release/v<version>`,
   sets the release version and commits + tags `v<version>` on it (crediting you as
   co-author), publishes `corral-sdk`, `corral-rules` and `corral-parent` to
   GitHub Packages — consumers need the parent pom to resolve the managed dependency
   versions — bumps to
   `<nextVersion>-SNAPSHOT`, pushes the branch + tag, creates the GitHub Release, then opens a
   PR (`release/v<version>` → `main`) and enables **auto-merge**. The PR merges automatically
   once the required build check passes.

Before publishing, the workflow runs `./mvnw clean verify` on the version-bumped tree. That tree
has never been built by any CI run — `versions:set` has just rewritten the poms — and publishing to
GitHub Packages cannot be undone, so the tests and the coverage gate run against exactly what is
about to be released. The `deploy` step itself then uses `-DskipTests=true` rather than testing
twice.

`corral-example` is never published — its `maven-deploy-plugin` is skipped, and the
workflow's already-published check skips it for the same reason.

The trigger allowlist is hardcoded in `.github/workflows/release.yml` as
`RELEASE_ALLOWED_ACTORS` (space-separated GitHub usernames); edit it via a normal PR.

## Snapshots

Release publishes released `x.y.z` versions. Snapshots are published manually: dispatch
**Publish artifact** on a ref whose project version ends in `-SNAPSHOT` (`main`, typically). GitHub
Packages rejects re-deploying an existing release version but accepts re-deploying a snapshot, so
`0.1.0-SNAPSHOT` can be refreshed as often as needed — which is what makes it useful for trying a
change before a release is cut.
