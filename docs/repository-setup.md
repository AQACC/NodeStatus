# Repository Setup

Use this checklist before the first public push and after creating the GitHub
repository.

## First Public Push

1. Create the remote repository.
2. If the final repository name is not `NodeStatus`, update:
   - `README.md` badge URLs
   - `SECURITY.md` advisory URL
   - `.github/ISSUE_TEMPLATE/config.yml`
3. Push `main` and confirm all GitHub Actions workflows appear.

## GitHub Security Settings

Enable these repository features:

- Private vulnerability reporting
- Dependency graph
- Dependabot alerts
- Dependabot security updates, if available for your plan

## Branch Protection

For the `main` branch, enable these rules:

- Require a pull request before merging
- Require at least one approval
- Require status checks to pass before merging
- Required checks:
  - `Android CI / build`
  - `DCO / dco`
  - `Secret Scan / gitleaks`
- Dismiss stale approvals when new commits are pushed
- Disallow force pushes
- Disallow branch deletion

## Release Discipline

- Keep `CHANGELOG.md` updated from `Unreleased`
- Treat all releases before 1.0 as potentially breaking
- Tag only commits that pass `./gradlew test lint assembleDebug`
- Follow [docs/release-process.md](release-process.md) when publishing signed APKs to GitHub Releases

## CI Stability

The repository currently targets Android SDK 36 and AGP 9.1 because the local
baseline build passes with that pair.

If GitHub runners start failing during `sdkmanager` install or Gradle plugin
resolution, downgrade the SDK and AGP together to the latest stable pair and
update both of these files in the same change:

- `.github/workflows/android.yml`
- `gradle/libs.versions.toml`

After any version change, rerun:

```text
./gradlew test lint assembleDebug
```
