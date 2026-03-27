# Contributing

## Before you open a PR

1. Open an issue if the change alters the monitoring model, rule-pack contract, or security boundaries.
2. Keep each PR focused on one concern: foundation, adapter contract, fixture updates, or UI integration.
3. Do not include real credentials, customer domains, or raw provider payloads.

## What contributors should add

Code contributions:

- Keep Android UI thin and configuration-oriented.
- Put provider-agnostic concepts in `core/*`.
- Put provider-specific behavior in `adapter/*`.
- Add or update tests with the same change.

Rule-pack contributions:

- Add a pack under `rule-packs/packs/<family>.<profile>/`.
- Include a `manifest.yaml`.
- Document capabilities and auth expectations.
- Attach sanitized fixtures and explain what was redacted.

## Review checklist

- The change matches the documented architecture.
- No secrets or identifying customer data are present.
- Fixtures are sanitized and reproducible.
- The baseline commands pass locally:

```text
./gradlew test lint assembleDebug
```

## Branching and commits

- Use short-lived feature branches.
- Keep commits atomic and reversible.
- Prefer descriptive commit messages over large mixed commits.
- Sign off every commit (DCO):

```text
git commit -s
```

## Out of scope for community plugins

The Android client must not execute arbitrary third-party code.
Community extension points are rule packs, fixtures, documentation, and reviewed source changes.
