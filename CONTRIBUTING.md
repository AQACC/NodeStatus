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

## Line endings on Windows

- Treat `.gitattributes` as the source of truth for line endings.
- Keep `*.bat` and `*.cmd` files as CRLF. Most other text files in the repository should remain LF.
- Do not open a PR with a wrapper-script diff unless you intended to change the script contents.
- If `gradlew.bat` shows as modified unexpectedly, verify the diff first:

```text
git diff --ignore-cr-at-eol -- gradlew.bat
```

- If the file only needs normalization, restage it with:

```text
git add --renormalize gradlew.bat
```

## Out of scope for community plugins

The Android client must not execute arbitrary third-party code.
Community extension points are rule packs, fixtures, documentation, and reviewed source changes.
