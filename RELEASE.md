# Release Guide

This document explains how to create a new release for Aido using GitHub Actions.

## Setup (One-time)

### 1. Generate Signing Key

If you don't have a signing key, create one:

```bash
keytool -genkey -v -keystore aido-release.keystore -alias aido -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Encode Keystore to Base64

```bash
# On Linux/Mac
base64 -i aido-release.keystore -o keystore.b64

# On Windows (PowerShell)
certutil -encode aido-release.keystore keystore.b64
```

### 3. Add GitHub Secrets

Go to your repository → Settings → Secrets and variables → Actions → New repository secret

Add these secrets:
- `SIGNING_KEY`: Content of `keystore.b64` file
- `KEY_ALIAS`: Your key alias (e.g., "aido")
- `KEY_STORE_PASSWORD`: Your keystore password
- `KEY_PASSWORD`: Your key password

## Creating a Release

### Method 1: Using Git Commands

```bash
# 1. Update version in app/build.gradle.kts
# 2. Commit your changes
git add .
git commit -m "Bump version to 1.0.0"

# 3. Create and push a tag
git tag v1.0.0
git push origin v1.0.0
```

### Method 2: Using GitHub UI

1. Go to your repository on GitHub
2. Click on "Releases" → "Draft a new release"
3. Click "Choose a tag" → Type `v1.0.0` → "Create new tag on publish"
4. Fill in release title: `Aido v1.0.0`
5. Click "Publish release"

## What Happens Next

GitHub Actions will automatically:
1. ✅ Build the release APK
2. ✅ Sign the APK with your keystore
3. ✅ Generate changelog from commits
4. ✅ Create a GitHub release
5. ✅ Upload the signed APK to the release
6. ✅ Keep APK as artifact for 90 days

## Version Naming Convention

- **Major releases**: v1.0.0, v2.0.0 (Breaking changes)
- **Minor releases**: v1.1.0, v1.2.0 (New features)
- **Patch releases**: v1.0.1, v1.0.2 (Bug fixes)
- **Beta releases**: v1.0.0-beta.1 (Testing)

## Troubleshooting

### Build Failed
- Check the Actions tab for error logs
- Ensure all secrets are correctly set
- Verify your `build.gradle.kts` syntax

### Signing Failed
- Verify `SIGNING_KEY` is base64 encoded correctly
- Check that passwords match your keystore
- Ensure key alias is correct

### Release Not Created
- Check if you have write permissions
- Ensure `GITHUB_TOKEN` has necessary permissions
- Look for errors in the Actions log

## Manual Release (Fallback)

If GitHub Actions fails, you can build manually:

```bash
# Build release APK
./gradlew assembleRelease

# Sign APK (if unsigned)
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
  -keystore aido-release.keystore \
  app/build/outputs/apk/release/app-release-unsigned.apk aido

# Optimize APK
zipalign -v 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  aido-release.apk
```

Then manually upload to GitHub Releases.
