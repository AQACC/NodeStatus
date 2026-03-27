# Governance

## Project Status

NodeStatus is pre-1.0 and under active design. Breaking changes are expected
while the architecture and provider contract are still stabilizing.

## Roles

- Maintainer: Reviews and merges pull requests, manages releases, and enforces
  project policies.
- Contributor: Proposes and submits code, docs, fixtures, and rule-pack changes.

## Decision Process

- Normal changes are merged through pull requests after maintainer review and
  passing CI.
- Architecture, security boundary, or rule-pack contract changes should be
  discussed in an issue before implementation.
- When consensus is unclear, the maintainer makes the final decision.

## Repository Settings

These settings are enforced in GitHub repository configuration rather than in
source files.

- Require pull requests for `main`.
- Require status checks for Android CI, DCO, and Secret Scan.
- Require at least one approving review.
- Disallow force pushes and branch deletions on `main`.

See [docs/repository-setup.md](docs/repository-setup.md) for the exact checklist.

## Contribution Policy

- All commits in pull requests must include a DCO sign-off
  (`Signed-off-by` line).
- Contributions must follow [CONTRIBUTING.md](CONTRIBUTING.md),
  [SECURITY.md](SECURITY.md), and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
- The project does not accept runtime execution of arbitrary third-party code in
  the Android client.

## Security and Data Handling

- Never commit live credentials, cookies, tokens, or customer-identifying data.
- Fixtures and captured payloads must be sanitized before review.
- Security vulnerabilities must be reported privately according to
  [SECURITY.md](SECURITY.md).

## Release and Compatibility

- `main` is the primary integration branch.
- Releases before 1.0 may include breaking changes without deprecation windows.
- Keep `CHANGELOG.md` updated before tagging a release.
- Compatibility guarantees will be defined when the project reaches a stable
  provider contract.
