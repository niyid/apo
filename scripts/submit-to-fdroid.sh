#!/bin/bash

# scripts/submit-to-fdroid.sh
# Complete workflow for F-Droid submission

set -e

echo "=========================================="
echo "F-Droid Submission Helper"
echo "=========================================="
echo ""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Check if we're on fdroid branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "fdroid" ]; then
    echo -e "${RED}ERROR: Must be on fdroid branch${NC}"
    echo "Current branch: $CURRENT_BRANCH"
    exit 1
fi

echo -e "${BLUE}This script will guide you through F-Droid submission${NC}"
echo ""

# 1. Pre-submission checklist
echo "=========================================="
echo "Pre-Submission Checklist"
echo "=========================================="
echo ""

CHECKLIST=(
    "App builds successfully with ./gradlew assembleRelease"
    "All unit tests pass"
    "No Firebase or Google Play Services dependencies"
    "Fastlane metadata is complete"
    "App has been tested on a real device"
    "LICENSE file exists"
    "README is up to date"
    "Version code and name are correct in gradle.properties"
    "No sensitive files (keystores, API keys) in repository"
    "All images/screenshots are ready"
)

echo "Please confirm you have completed these items:"
echo ""

for item in "${CHECKLIST[@]}"; do
    echo "☐ $item"
done

echo ""
read -p "Have you completed all items above? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Please complete the checklist before proceeding."
    exit 1
fi

# 2. Run validation
echo ""
echo "=========================================="
echo "Running Validation"
echo "=========================================="
echo ""

if [ -f "scripts/validate-fdroid.sh" ]; then
    if ./scripts/validate-fdroid.sh; then
        echo -e "${GREEN}✓ Validation passed${NC}"
    else
        echo -e "${RED}✗ Validation failed${NC}"
        exit 1
    fi
else
    echo -e "${RED}ERROR: Validation script not found${NC}"
    exit 1
fi

# 3. Generate F-Droid metadata template
echo ""
echo "=========================================="
echo "Generating F-Droid Metadata"
echo "=========================================="
echo ""

APP_ID="com.techducat.apo"
VERSION_NAME=$(grep VERSION_NAME gradle.properties | cut -d'=' -f2)
VERSION_CODE=$(grep VERSION_CODE gradle.properties | cut -d'=' -f2)
REPO_URL="https://github.com/niyid/apo"

METADATA_FILE="fdroid-metadata.yml"

cat > "$METADATA_FILE" << EOF
# F-Droid metadata for Àpò
# Submit this to: https://gitlab.com/fdroid/rfp/-/issues

Categories:
 - Money

License: MIT

AuthorName: TechDucat
AuthorEmail: neeyeed@gmail.com

WebSite: $REPO_URL
SourceCode: $REPO_URL
IssueTracker: $REPO_URL/issues

AutoName: Àpò

Summary: Privacy-focused Monero wallet with modern Material Design 3 interface

Description: |-
    Àpò is a native Monero wallet application for Android built with modern 
    technologies including Kotlin and Jetpack Compose, offering a secure and 
    user-friendly way to manage your XMR cryptocurrency.
    
    Features:
    * Create new Monero wallets or restore existing ones using seed phrases
    * Send and receive XMR transactions with ease
    * Real-time blockchain synchronization
    * Complete transaction history with powerful search functionality
    * Blockchain rescan capability for wallet recovery
    * QR code generation and scanning for seamless address sharing
    * Biometric authentication for enhanced security
    * Modern, intuitive Material Design 3 interface
    * Multi-language support
    * 100% FOSS - No proprietary dependencies
    
    Privacy & Security:
    * Native Monero C++ libraries ensure protocol compatibility
    * All wallet data encrypted and stored locally on your device
    * No analytics or tracking
    * Biometric authentication protects wallet access
    * Open source and auditable

RepoType: git
Repo: $REPO_URL
Binaries: 

Builds:
  - versionName: $VERSION_NAME
    versionCode: $VERSION_CODE
    commit: fdroid-v$VERSION_NAME
    subdir: app
    sudo:
      - apt-get update
      - apt-get install -y python3
    gradle:
      - yes
    prebuild:
      - echo "Building F-Droid version"
    scanignore:
      - app/src/main/jniLibs
    scandelete:
      - app/build
    ndk: r21e

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: $VERSION_NAME
CurrentVersionCode: $VERSION_CODE
EOF

echo -e "${GREEN}✓ Generated $METADATA_FILE${NC}"
echo ""

# 4. Create submission checklist document
echo "Creating submission documentation..."

SUBMISSION_DOC="FDROID_SUBMISSION.md"

cat > "$SUBMISSION_DOC" << 'EOF'
# F-Droid Submission Guide for Àpò

## Step 1: Prepare the Branch

✅ Created fdroid branch
✅ Removed all proprietary dependencies
✅ Updated build.gradle.kts
✅ Created Fastlane metadata
✅ Validated build

## Step 2: Create Release Tag

Create a git tag for the F-Droid build:

```bash
git tag -a fdroid-v0.0.31 -m "F-Droid release v0.0.31"
git push origin fdroid-v0.0.31
```

## Step 3: Submit to F-Droid

### Option A: Request for Packaging (RFP)

1. Go to: https://gitlab.com/fdroid/rfp/-/issues
2. Click "New Issue"
3. Title: "Àpò - Monero Wallet"
4. Use the template and include information from `fdroid-metadata.yml`
5. Submit

### Option B: Direct Metadata Submission (Advanced)

1. Fork: https://gitlab.com/fdroid/fdroiddata
2. Create new file: `metadata/com.techducat.apo.yml`
3. Copy contents from `fdroid-metadata.yml`
4. Submit merge request

## Step 4: Monitor the Submission

- Watch for comments on your issue/MR
- Respond to any questions from F-Droid maintainers
- Be patient - review can take several weeks

## Step 5: After Acceptance

Once accepted:
- App will be built by F-Droid infrastructure
- Updates will be automatic based on git tags
- Monitor the F-Droid build logs for any issues

## Updating the App

For future updates:

1. Update version in `gradle.properties`
2. Update changelog in `fastlane/metadata/android/en-US/changelogs/XX.txt`
3. Commit changes to fdroid branch
4. Create new tag: `git tag fdroid-v0.0.XX`
5. Push: `git push origin fdroid fdroid-v0.0.XX`
6. F-Droid will automatically detect and build the new version

## Common Issues

### Build Failures

- Check F-Droid build logs
- Ensure all dependencies are from Maven Central or Google Maven
- Native libraries must be properly configured

### Metadata Issues

- Follow F-Droid metadata reference strictly
- Use SPDX license identifiers
- Keep descriptions objective

### Signing Issues

- F-Droid handles signing
- Don't include signing configs in fdroid branch

## Resources

- F-Droid Documentation: https://f-droid.org/docs/
- Build Metadata Reference: https://f-droid.org/docs/Build_Metadata_Reference/
- RFP Process: https://gitlab.com/fdroid/rfp
- F-Droid Data Repo: https://gitlab.com/fdroid/fdroiddata
EOF

echo -e "${GREEN}✓ Created $SUBMISSION_DOC${NC}"
echo ""

# 5. Final instructions
echo "=========================================="
echo "Submission Files Ready!"
echo "=========================================="
echo ""

echo "Files created:"
echo "  1. $METADATA_FILE - F-Droid metadata"
echo "  2. $SUBMISSION_DOC - Submission guide"
echo "  3. fdroid-build-report.txt - Latest build report (if you ran test script)"
echo ""

echo -e "${BLUE}Next Steps:${NC}"
echo ""
echo "1. Create a release tag:"
echo "   ${YELLOW}git tag -a fdroid-v$VERSION_NAME -m 'F-Droid release v$VERSION_NAME'${NC}"
echo "   ${YELLOW}git push origin fdroid fdroid-v$VERSION_NAME${NC}"
echo ""
echo "2. Submit to F-Droid using one of these methods:"
echo ""
echo "   ${GREEN}Option A - Request for Packaging (Easier):${NC}"
echo "   - Visit: https://gitlab.com/fdroid/rfp/-/issues"
echo "   - Create new issue with title: 'Àpò - Monero Wallet'"
echo "   - Copy content from: $METADATA_FILE"
echo ""
echo "   ${GREEN}Option B - Direct Merge Request (Advanced):${NC}"
echo "   - Fork: https://gitlab.com/fdroid/fdroiddata"
echo "   - Create: metadata/com.techducat.apo.yml"
echo "   - Submit merge request"
echo ""
echo "3. Read the full guide:"
echo "   ${YELLOW}cat $SUBMISSION_DOC${NC}"
echo ""

# 6. Ask about pushing
echo "=========================================="
echo ""
read -p "Would you like to push the fdroid branch now? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "Pushing fdroid branch to origin..."
    git push -u origin fdroid
    echo ""
    read -p "Create and push release tag fdroid-v$VERSION_NAME? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git tag -a "fdroid-v$VERSION_NAME" -m "F-Droid release v$VERSION_NAME"
        git push origin "fdroid-v$VERSION_NAME"
        echo -e "${GREEN}✓ Tagged and pushed fdroid-v$VERSION_NAME${NC}"
    fi
fi

echo ""
echo "=========================================="
echo -e "${GREEN}All Done! 🎉${NC}"
echo "=========================================="
echo ""
echo "Your app is ready for F-Droid submission!"
echo ""
