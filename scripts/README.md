# F-Droid Submission Scripts

This directory contains scripts to help prepare and submit Àpò to F-Droid.

## Quick Start

To set up the F-Droid branch in one command:

```bash
chmod +x setup-fdroid-branch.sh
./setup-fdroid-branch.sh
```

This will automatically:
1. Create the `fdroid` branch
2. Run all preparation scripts
3. Set up Fastlane metadata
4. Clean up proprietary dependencies

## Scripts Overview

### 1. `setup-fdroid-branch.sh` - Main Setup Script
**Purpose**: Creates and initializes the F-Droid branch

**Usage**:
```bash
./setup-fdroid-branch.sh
```

**What it does**:
- Creates a new `fdroid` branch from your current branch
- Runs all preparation scripts automatically
- Sets up the complete F-Droid-compatible structure

**When to use**: First time setting up F-Droid branch

---

### 2. `prepare-fdroid.sh` - Preparation Script
**Purpose**: Modifies files for F-Droid compatibility

**Usage**:
```bash
./scripts/prepare-fdroid.sh
```

**What it does**:
- Updates `app/build.gradle.kts` to remove proprietary dependencies
- Removes Firebase and Google Play Services
- Replaces ML Kit with ZXing
- Creates Fastlane metadata structure
- Generates F-Droid documentation

**When to use**: When setting up fdroid branch or updating configuration

---

### 3. `cleanup-fdroid.sh` - Cleanup Script
**Purpose**: Removes proprietary files and build artifacts

**Usage**:
```bash
./scripts/cleanup-fdroid.sh
```

**What it does**:
- Removes `google-services.json`
- Deletes keystore files
- Cleans build artifacts
- Updates `.gitignore`
- Scans for Firebase/ML Kit imports in source code

**When to use**: 
- After making changes to ensure no proprietary files remain
- Before committing to fdroid branch
- Periodically to keep branch clean

---

### 4. `validate-fdroid.sh` - Validation Script
**Purpose**: Validates F-Droid compatibility

**Usage**:
```bash
./scripts/validate-fdroid.sh
```

**What it does**:
- Checks for proprietary dependencies
- Validates Fastlane metadata exists
- Verifies build configuration
- Scans for sensitive files
- Provides detailed report

**Output**: 
- ✓ PASS: No issues found
- ⚠ WARNING: Non-critical issues
- ✗ ERROR: Must be fixed

**When to use**:
- Before committing changes
- Before submitting to F-Droid
- After any dependency updates

---

### 5. `test-fdroid-build.sh` - Build Testing Script
**Purpose**: Comprehensive build testing

**Usage**:
```bash
./scripts/test-fdroid-build.sh
```

**What it does**:
- Runs clean build
- Tests debug and release builds
- Runs unit tests
- Analyzes APK for proprietary dependencies
- Runs lint checks
- Generates build report

**Output**: 
- Build artifacts in `app/build/outputs/apk/`
- Test reports in `app/build/reports/`
- Build report: `fdroid-build-report.txt`

**When to use**:
- Before submitting to F-Droid
- After major changes
- To verify everything works

---

### 6. `submit-to-fdroid.sh` - Submission Helper
**Purpose**: Guides through F-Droid submission process

**Usage**:
```bash
./scripts/submit-to-fdroid.sh
```

**What it does**:
- Runs pre-submission checklist
- Validates the build
- Generates F-Droid metadata file
- Creates submission documentation
- Optionally pushes branch and creates tags

**Output**:
- `fdroid-metadata.yml` - For F-Droid submission
- `FDROID_SUBMISSION.md` - Complete submission guide

**When to use**: When ready to submit to F-Droid

---

## Complete Workflow

### Initial Setup (One Time)

```bash
# 1. Create and set up fdroid branch
./setup-fdroid-branch.sh

# 2. Review changes
git status
git diff

# 3. Test the build
./scripts/test-fdroid-build.sh

# 4. Commit changes
git add .
git commit -m "Prepare F-Droid branch"
git push -u origin fdroid
```

### Making Updates

```bash
# 1. Switch to fdroid branch
git checkout fdroid

# 2. Make your changes
# ... edit files ...

# 3. Clean up
./scripts/cleanup-fdroid.sh

# 4. Validate
./scripts/validate-fdroid.sh

# 5. Test
./scripts/test-fdroid-build.sh

# 6. Commit
git commit -am "Update for F-Droid"
```

### Submitting to F-Droid

```bash
# 1. Ensure everything is up to date
git checkout fdroid
git pull origin fdroid

# 2. Update version in gradle.properties
# VERSION_CODE=32
# VERSION_NAME=0.0.32

# 3. Update changelog
# Create: fastlane/metadata/android/en-US/changelogs/32.txt

# 4. Run submission helper
./scripts/submit-to-fdroid.sh

# 5. Follow the instructions it provides
```

## Script Dependencies

All scripts require:
- **bash** (standard on Linux/Mac)
- **git** (for branch management)
- **grep, sed, awk** (standard Unix tools)

Optional but recommended:
- **aapt** (Android Asset Packaging Tool) - for APK analysis
- **jarsigner** (Java) - for signature verification
- **unzip** - for APK inspection

## Troubleshooting

### Permission Denied

```bash
chmod +x setup-fdroid-branch.sh
chmod +x scripts/*.sh
```

### Script Not Found

Ensure you're in the project root directory:
```bash
cd /path/to/apo
./setup-fdroid-branch.sh
```

### Validation Errors

Run cleanup first:
```bash
./scripts/cleanup-fdroid.sh
./scripts/validate-fdroid.sh
```

### Build Failures

Check for:
1. Proprietary dependencies still present
2. Missing FOSS alternatives
3. Firebase/ML Kit imports in code

Run:
```bash
./scripts/cleanup-fdroid.sh
grep -r "firebase" app/src/main/
grep -r "mlkit" app/src/main/
```

## File Structure After Setup

```
apo/
├── scripts/
│   ├── README.md (this file)
│   ├── prepare-fdroid.sh
│   ├── cleanup-fdroid.sh
│   ├── validate-fdroid.sh
│   ├── test-fdroid-build.sh
│   └── submit-to-fdroid.sh
├── setup-fdroid-branch.sh
├── fastlane/
│   └── metadata/
│       └── android/
│           └── en-US/
│               ├── title.txt
│               ├── short_description.txt
│               ├── full_description.txt
│               ├── changelogs/
│               │   └── 31.txt
│               └── images/
│                   └── phoneScreenshots/
├── FDROID.md
├── README.fdroid.md
├── .fdroidignore
└── fdroid-metadata.yml (generated)
```

## Getting Help

- Review script output carefully - they provide detailed error messages
- Check `fdroid-build-report.txt` after running tests
- Read `FDROID_SUBMISSION.md` for submission guidance
- F-Droid documentation: https://f-droid.org/docs/

## Contributing

To improve these scripts:
1. Test thoroughly before committing
2. Add clear comments
3. Follow existing error handling patterns
4. Update this README with any changes

## License

These scripts are part of Àpò and licensed under MIT.
