# NodeStatus

NodeStatus is an Android configuration center for panel-based infrastructure monitoring.
The long-term goal is to support multiple hosting panels through reviewed rule packs,
then surface the resulting snapshots through widgets, notifications, and compact app flows.

The repository is still in the foundation phase. The first validated provider family is
`VirtFusion`, with `vmvm` as the initial site profile target.

## Current priorities

1. Stabilize the project foundation before feature work.
2. Define the core monitoring model and rule-pack contract.
3. Prove the first `VirtFusion` adapter path with sanitized fixtures.
4. Keep Android entry points thin and let widgets read local snapshots only.

## Repository layout

- `app/`: Android application shell and future configuration-center UI.
- `core/model/`: Provider-agnostic monitoring model.
- `core/storage/`: Snapshot persistence contracts.
- `core/widget/`: Widget-facing projection contracts.
- `adapter/engine/`: Rule-pack manifest and adapter contracts.
- `adapter/virtfusion/`: First provider-family blueprint.
- `adapter/testkit/`: Fixture and adapter-test helpers.
- `rule-packs/`: Community-facing rule-pack samples and schemas.
- `docs/`: Architecture and contribution documentation.

## Local setup

1. Install Android Studio stable on Windows.
2. Open `D:\Achieve\Code\Android\NodeStatus`.
3. Use JDK 17 for Gradle in Android Studio, even if newer JDKs are installed on the machine.
4. Install Android SDK Platform 36, Build-Tools 36.0.0, Platform-Tools, and Command-line Tools.
5. Prefer a real device for widget, notification, and WebView validation.

## Local session auth

For local `VirtFusion` session-backed development, copy
`local.auth.properties.example` to `local.auth.properties` and keep it untracked.
See [docs/local-session-auth.md](docs/local-session-auth.md) and
[docs/real-request-auth.md](docs/real-request-auth.md).

## Baseline commands

Windows:

```powershell
.\gradlew.bat test lint assembleDebug
```

macOS/Linux:

```bash
./gradlew test lint assembleDebug
```

## Contribution rules

- Never commit real API keys, cookies, tokens, or raw customer data.
- New adapters should start with sanitized fixtures and rule-pack documentation.
- Widget and notification surfaces must read cached snapshots, not direct network calls.
- Community contributions should favor declarative rule packs over runtime-executed third-party code.

## License

This repository is licensed under the [MIT License](LICENSE).

Read [CONTRIBUTING.md](CONTRIBUTING.md), [SECURITY.md](SECURITY.md),
[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), [GOVERNANCE.md](GOVERNANCE.md),
[docs/architecture.md](docs/architecture.md), [docs/rule-pack-format.md](docs/rule-pack-format.md),
and [docs/license-selection.md](docs/license-selection.md)
before opening a pull request.
