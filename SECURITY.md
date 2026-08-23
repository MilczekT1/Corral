# Security Policy

## Reporting a vulnerability

**Please do not open a public issue for a security vulnerability.**

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/MilczekT1/Corral/security/advisories/new).
That opens a draft advisory visible only to you and the maintainers.

If you cannot use that form, email <konrad_boniecki@hotmail.com> with `CORRAL SECURITY` in the
subject line.

Please include:

- what an attacker can do, and what they need to already have in order to do it
- the affected version, and the ArchUnit and JDK versions you observed it on
- a minimal reproduction if you have one

**Response times.** This is a solo-maintained project, so treat these as intent rather than a
guarantee: acknowledgement within 7 days, an assessment within 14. If you have not heard back within
7 days, please ping the advisory thread — it means the notification was missed, not ignored.

You will be credited in the advisory unless you ask not to be.

## Why this matters for a build-time library

Corral runs inside its consumers' builds and its whole job is to be trusted by one. Two classes of
problem are in scope even though they are not memory-safety bugs:

- **Silent non-enforcement.** Anything that makes a rule pass without evaluating — a freeze-store key
  mismatch, a group that consumers never reach, a swallowed import failure. The consumer gets a green
  build and zero enforcement, which is worse than a red one because nobody looks.
- **Build-time execution.** Corral is a test-scoped dependency, so its code runs on developer machines
  and CI runners with whatever credentials those hold.

Report either of these through the private channel above, not as a normal issue.

## Supply chain

- Third-party GitHub Actions are pinned to full commit SHAs.
- Dependabot watches GitHub Actions and Maven dependencies.
- Secret scanning and push protection are enabled on the repository.

## Supported versions

Nothing has been released yet. Once `0.1.0` ships, security fixes will target the latest minor
release; this section will be replaced with a concrete table at that point.
