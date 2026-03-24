# Rule Pack Format

## Goal

Rule packs let contributors describe provider or site-specific behavior without shipping arbitrary runtime code.
The first version is intentionally small and reviewable.

## Directory shape

```text
rule-packs/
  schema/
    manifest.schema.json
  packs/
    virtfusion.vmvm/
      manifest.yaml
      capabilities.yaml
      fixtures/
        README.md
```

## Required files

`manifest.yaml`

- unique pack id
- provider family
- display name
- semantic version
- supported auth methods
- declared capabilities
- fixture directory

`capabilities.yaml`

- the metrics or actions the pack claims to support
- notes about missing or partial support

`fixtures/`

- sanitized responses or screenshots needed for parser and mapping tests
- a short note describing what was redacted

## Review rules

- Rule packs must be deterministic and reviewable.
- Credentials never belong in a rule pack.
- Customer identifiers must be removed or replaced.
- Pack ids should follow `<family>.<profile>`.

## Evolution policy

The manifest schema will evolve conservatively.
Breaking changes should be versioned explicitly and accompanied by migration notes.
