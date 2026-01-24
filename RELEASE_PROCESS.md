# Aido Release Process Guide

> **Personal Reference Documentation**  
> This guide covers the complete process of making changes, committing to GitHub, and creating releases for the Aido project.

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Development Workflow](#development-workflow)
3. [Committing Changes](#committing-changes)
4. [Creating a Release](#creating-a-release)
5. [Version Management](#version-management)
6. [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before starting, ensure you have:

- ✅ Git installed and configured
- ✅ Android Studio or VS Code with Kotlin support
- ✅ GitHub account with repository access
- ✅ GitHub Actions enabled (automatic via workflows)

---

## Development Workflow

### Step 1: Make Your Code Changes

1. Open Android Studio or your preferred IDE
2. Navigate to the files you want to modify
3. Make your changes (bug fixes, new features, UI improvements, etc.)
4. Test locally to ensure everything works

### Step 2: Update Version (For Releases Only)

**Location:** `app/build.gradle.kts`

```kotlin
defaultConfig {
    applicationId = "com.rr.aido"
    minSdk = 24
    targetSdk = 36
    versionCode = 61     // Increment this by 1
    versionName = "4.1"  // Update according to semantic versioning
}
```

**Version Naming Convention:**
- **Major (X.0.0)**: Breaking changes, major new features
- **Minor (x.X.0)**: New features, no breaking changes
- **Patch (x.x.X)**: Bug fixes, minor improvements

**Version Code Rule:** Always increment by 1 for each release (60 → 61 → 62...)

---

## Committing Changes

### Step 1: Check What Changed

```powershell
cd C:\Users\admin\Pictures\aido
git status
```

This shows all modified, added, or deleted files.

### Step 2: Stage Your Changes

```powershell
# Add all changes
git add .

# Or add specific files
git add app/src/main/java/com/rr/aido/ui/screens/HomeScreen.kt
```

### Step 3: Commit with a Meaningful Message

Use **Conventional Commits** format:

```powershell
git commit -m "type: brief description"
```

**Commit Types:**
- `feat:` - New feature
- `fix:` - Bug fix
- `style:` - UI/UX changes
- `docs:` - Documentation changes
- `refactor:` - Code restructuring
- `perf:` - Performance improvements
- `test:` - Adding tests
- `chore:` - Build process, dependencies

**Examples:**
```powershell
git commit -m "feat: Add dark mode support"
git commit -m "fix: Resolve crash on startup"
git commit -m "style: Update HomeScreen UI design"
git commit -m "docs: Update README with new features"
```

### Step 4: Push to GitHub

```powershell
git push
```

If it's your first push or you want to set upstream:
```powershell
git push -u origin main
```

---

## Creating a Release

### Automatic Release Process

The project uses **GitHub Actions** to automatically build, sign, and release APKs when you push a version tag.

### Step 1: Ensure All Changes Are Committed

```powershell
git status  # Should show "nothing to commit"
```

### Step 2: Create and Push a Version Tag

```powershell
# Create tag (use your version number)
git tag v4.1

# Push tag to GitHub
git push origin v4.1
```

**Tag Naming:** Always prefix with `v` (e.g., `v4.1`, `v4.2`, `v5.0`)

### Step 3: Wait for GitHub Actions

1. Go to: `https://github.com/MyAido/Aido/actions`
2. You'll see two workflows running:
   - **Android CI Build** (Debug APK - for testing)
   - **Android Release Build** (Signed APK - for distribution)

3. Wait for both to complete (approximately 5-10 minutes)

### Step 4: Verify the Release

1. Go to: `https://github.com/MyAido/Aido/releases`
2. You should see your new release with:
   - Release title: `Aido v4.1`
   - Auto-generated changelog
   - Downloadable APK file: `aido-4.1.apk`

---

## Version Management

### Planning Version Updates

| Type of Change | Version Example | Version Code |
|----------------|-----------------|--------------|
| Major redesign, breaking changes | 4.0 → 5.0 | 60 → 65 |
| New features | 4.0 → 4.1 | 60 → 61 |
| Bug fixes | 4.1 → 4.1.1 | 61 → 62 |

### Update Checklist

Before creating a release, ensure:

- [ ] Version code incremented in `app/build.gradle.kts`
- [ ] Version name updated in `app/build.gradle.kts`
- [ ] All features tested locally
- [ ] README.md updated (if needed)
- [ ] All changes committed and pushed
- [ ] No failing tests or lint errors

---

## Complete Release Example

Here's a complete example of releasing version 4.2:

```powershell
# 1. Update version in app/build.gradle.kts
#    versionCode = 62
#    versionName = "4.2"

# 2. Stage all changes
git add .

# 3. Commit with descriptive message
git commit -m "feat: Add new AI model selection and improve keyboard performance

- Added support for multiple AI models
- Optimized keyboard response time
- Fixed memory leak in accessibility service
- Updated UI with new color scheme"

# 4. Push changes
git push

# 5. Create and push release tag
git tag v4.2
git push origin v4.2

# 6. Go to GitHub Actions and monitor the build
# 7. Once complete, check Releases tab for the new APK
```

---

## Troubleshooting

### Issue: Tag Already Exists

If you need to recreate a tag:

```powershell
# Delete local tag
git tag -d v4.1

# Delete remote tag
git push origin :refs/tags/v4.1

# Create tag again
git tag v4.1

# Push updated tag
git push origin v4.1
```

### Issue: GitHub Actions Failed

1. Go to Actions tab
2. Click on the failed workflow
3. Check the error logs
4. Fix the issue in your code
5. Commit and push the fix
6. Recreate the tag (see above)

### Issue: APK Not Signed

Ensure the following secrets are set in GitHub:
- `SIGNING_KEY`
- `KEY_ALIAS`
- `KEY_STORE_PASSWORD`
- `KEY_PASSWORD`

Go to: `Settings → Secrets and variables → Actions`

### Issue: Permission Denied

The workflow includes `permissions: contents: write`, which should handle this automatically. If issues persist, check repository settings.

---

## Quick Reference Commands

```powershool
# Check status
git status

# Add all changes
git add .

# Commit changes
git commit -m "type: description"

# Push changes
git push

# Create release tag
git tag v4.X
git push origin v4.X

# View all tags
git tag -l

# Delete tag (local and remote)
git tag -d v4.X
git push origin :refs/tags/v4.X
```

---

## Additional Resources

- **Repository:** https://github.com/MyAido/Aido
- **Actions:** https://github.com/MyAido/Aido/actions
- **Releases:** https://github.com/MyAido/Aido/releases
- **Issues:** https://github.com/MyAido/Aido/issues

---

## Notes

- Always test locally before creating a release
- Use meaningful commit messages for better changelog generation
- Keep version numbers consistent between `build.gradle.kts` and git tags
- Monitor GitHub Actions for any build failures
- The automated workflow handles APK signing and release creation

---

**Last Updated:** January 24, 2026  
**Maintained by:** Senzme (MyAido)
