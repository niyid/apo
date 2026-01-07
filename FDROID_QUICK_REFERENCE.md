# F-Droid Quick Reference for Àpò

## 🚀 First Time Setup (5 minutes)

```bash
# 1. Make scripts executable
chmod +x setup-fdroid-branch.sh scripts/*.sh

# 2. Run setup (this does everything)
./setup-fdroid-branch.sh

# 3. Review and commit
git status
git add .
git commit -m "Setup F-Droid branch"
git push -u origin fdroid
```

## ✅ Pre-Submission Checklist

- [ ] On `fdroid` branch: `git checkout fdroid`
- [ ] Version updated in `gradle.properties`
- [ ] Changelog created for new version
- [ ] Build successful: `./gradlew assembleRelease`
- [ ] Tests pass: `./gradlew test`
- [ ] Validation passes: `./scripts/validate-fdroid.sh`
- [ ] App tested on device
- [ ] No proprietary files present

## 📝 Common Commands

### Validate Everything
```bash
./scripts/validate-fdroid.sh
```

### Test Build
```bash
./scripts/test-fdroid-build.sh
```

### Manual Build Commands
```bash
# Debug build
./gradlew assembleFdroidDebug

# Release build
./gradlew assembleFdroidRelease

# Run tests
./gradlew testFdroidDebugUnitTest

# Lint
./gradlew lintFdroidRelease
```

### Clean Up Branch
```bash
./scripts/cleanup-fdroid.sh
```

### Submit to F-Droid
```bash
./scripts/submit-to-fdroid.sh
```

## 🔄 Updating the App

```bash
# 1. Update version
vim gradle.properties  # Update VERSION_CODE and VERSION_NAME

# 2. Add changelog
vim fastlane/metadata/android/en-US/changelogs/32.txt

# 3. Validate and test
./scripts/validate-fdroid.sh
./scripts/test-fdroid-build.sh

# 4. Commit and tag
git commit -am "Version 0.0.32"
git tag fdroid-v0.0.32
git push origin fdroid fdroid-v0.0.32
```

## 🛠️ Troubleshooting

### "Permission denied"
```bash
chmod +x setup-fdroid-branch.sh scripts/*.sh
```

### "Validation failed"
```bash
./scripts/cleanup-fdroid.sh
./scripts/validate-fdroid.sh
```

### "Build failed"
```bash
./gradlew clean
./scripts/validate-fdroid.sh
./gradlew assembleFdroidRelease
```

### "Firebase still present"
```bash
./scripts/cleanup-fdroid.sh
grep -r "firebase" app/src/
```

## 📂 Important Files

| File | Purpose |
|------|---------|
| `app/build.gradle.kts` | F-Droid compatible build config |
| `fastlane/metadata/` | App store metadata |
| `FDROID.md` | Build instructions |
| `.fdroidignore` | Files to ignore |
| `fdroid-metadata.yml` | F-Droid submission metadata |

## 🌐 F-Droid Links

- **Submit App**: https://gitlab.com/fdroid/rfp/-/issues
- **Documentation**: https://f-droid.org/docs/
- **Metadata Reference**: https://f-droid.org/docs/Build_Metadata_Reference/

## 📊 Build Status Indicators

| Symbol | Meaning |
|--------|---------|
| ✓ | Passed |
| ⚠ | Warning (usually OK) |
| ✗ | Failed (must fix) |

## 🎯 Quick Fixes

### Remove Firebase
```bash
./scripts/cleanup-fdroid.sh
# Then remove imports from source code
```

### Update Metadata
```bash
vim fastlane/metadata/android/en-US/full_description.txt
```

### Test APK
```bash
./gradlew assembleFdroidRelease
adb install app/build/outputs/apk/fdroid/release/app-fdroid-release.apk
```

## 📱 Version Management

Current version info:
```bash
grep VERSION gradle.properties
```

Create release tag:
```bash
VERSION=$(grep VERSION_NAME gradle.properties | cut -d'=' -f2)
git tag -a fdroid-v$VERSION -m "F-Droid release v$VERSION"
git push origin fdroid-v$VERSION
```

## 🔍 Check for Issues

### Proprietary Dependencies
```bash
grep -E "(firebase|mlkit|gms)" app/build.gradle.kts
```

### Source Code Imports
```bash
grep -r "import com.google.firebase" app/src/
grep -r "import com.google.mlkit" app/src/
```

### Sensitive Files
```bash
find . -name "*.jks" -o -name "google-services.json"
```

## 💡 Tips

1. **Always validate** before pushing: `./scripts/validate-fdroid.sh`
2. **Test locally** before submitting: `./scripts/test-fdroid-build.sh`
3. **Keep main branch separate** - don't merge fdroid into main
4. **Tag releases** - F-Droid tracks by git tags
5. **Be patient** - F-Droid review takes time

## 📧 Support

- Issues: https://github.com/niyid/apo/issues
- F-Droid: https://gitlab.com/fdroid/rfp/-/issues

---

**Last Updated**: January 2026
**Àpò Version**: 0.0.31
**F-Droid Branch**: `fdroid`
