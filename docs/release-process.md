# Release Process

Use this flow when publishing signed APKs to GitHub Releases.

## What the workflow does

Pushing a tag that matches `v*` triggers `.github/workflows/release.yml`.
The workflow will:

- derive `versionName` and `versionCode` from the tag
- build the `release` APK
- sign it with your repository secrets
- generate a `.sha256` checksum file
- create a draft GitHub Release and upload both files

The release stays in `draft` state so you can review the notes and asset names before publishing.

## Supported tags

Use one of these tag shapes:

- `v0.1.0`
- `v0.1.0-alpha.1`
- `v0.1.0-beta.1`
- `v0.1.0-rc.1`

Rules for the current `versionCode` scheme:

- `minor` and `patch` must stay below `100`
- `alpha` and `beta` numbers must stay between `1` and `29`
- `rc` numbers must stay between `1` and `19`

Stable releases get a higher `versionCode` than prereleases of the same `major.minor.patch`.

## Required GitHub secrets

Add these repository secrets under `Settings > Secrets and variables > Actions`:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## Prepare the keystore secret

If you do not already have a release keystore, generate one locally and keep it backed up outside the repository.

Example command:

```text
keytool -genkeypair -v -keystore nodestatus-release.jks -alias nodestatus -keyalg RSA -keysize 4096 -validity 3650
```

Convert the `.jks` file to Base64 before saving it as `ANDROID_KEYSTORE_BASE64`.

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("nodestatus-release.jks")) | Set-Clipboard
```

macOS/Linux:

```bash
base64 -w 0 nodestatus-release.jks
```

## Publish a release

1. Merge the desired release commit to `main`.
2. Update `CHANGELOG.md` for the release.
3. Create an annotated tag:

```text
git tag -a v0.1.0-beta.1 -m "NodeStatus v0.1.0-beta.1"
```

4. Push the tag:

```text
git push origin v0.1.0-beta.1
```

5. Wait for the `Release APK` workflow to finish.
6. Open the generated draft release, review the notes, then publish it.

## If the workflow fails

- Confirm all four signing secrets exist and are non-empty.
- Confirm the tag matches one of the supported formats.
- Re-run the failed workflow after fixing secrets or tag issues.
- If you need to rebuild the same tag, re-run the workflow; the draft release assets are uploaded with `--clobber`.
