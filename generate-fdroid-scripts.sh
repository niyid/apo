#!/bin/bash

# generate-fdroid-scripts.sh
# Generates all missing F-Droid preparation scripts

set -e

echo "=========================================="
echo "F-Droid Scripts Generator"
echo "=========================================="
echo ""

PROJECT_ROOT=$(pwd)

# Create scripts directory if it doesn't exist
mkdir -p scripts

echo "Generating scripts..."
echo ""

# 1. organize-fdroid-graphics.sh (root level)
cat > organize-fdroid-graphics.sh << 'SCRIPT_EOF'
#!/bin/bash

# organize-fdroid-graphics.sh
# Organizes app graphics into Fastlane metadata structure for F-Droid

set -e

echo "=========================================="
echo "F-Droid Graphics Organization Script"
echo "=========================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Source directories
GRAPHICS_BASE="$HOME/Documents/apo"
LAUNCHER_ICONS="$GRAPHICS_BASE/android_launcher_bundle/res"
SCREENSHOTS_BASE="$GRAPHICS_BASE/drive-download-20251203T172314Z-1-001"

# Destination directory
FASTLANE_BASE="fastlane/metadata/android"

echo -e "${BLUE}Checking source directories...${NC}"

if [ ! -d "$GRAPHICS_BASE" ]; then
    echo -e "${RED}Error: Graphics base directory not found: $GRAPHICS_BASE${NC}"
    exit 1
fi

if [ ! -d "$SCREENSHOTS_BASE" ]; then
    echo -e "${RED}Error: Screenshots directory not found: $SCREENSHOTS_BASE${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Source directories found${NC}"
echo ""

# Create Fastlane directory structure
echo -e "${BLUE}Creating Fastlane directory structure...${NC}"

mkdir -p "$FASTLANE_BASE/en-US/images/phoneScreenshots"
mkdir -p "$FASTLANE_BASE/en-US/images/sevenInchScreenshots"
mkdir -p "$FASTLANE_BASE/en-US/images/tenInchScreenshots"
mkdir -p "$FASTLANE_BASE/en-US/changelogs"

echo -e "${GREEN}✓ Directory structure created${NC}"
echo ""

# Process app icon
echo -e "${BLUE}Processing app icon...${NC}"

ICON_SOURCE="$LAUNCHER_ICONS/drawable-xxxhdpi/ic_launcher_foreground.png"
ICON_DEST="$FASTLANE_BASE/en-US/images/icon.png"

if [ -f "$ICON_SOURCE" ]; then
    if command -v convert &> /dev/null; then
        convert "$ICON_SOURCE" -resize 512x512 "$ICON_DEST"
        echo -e "${GREEN}✓ App icon created (512x512)${NC}"
    else
        cp "$ICON_SOURCE" "$ICON_DEST"
        echo -e "${YELLOW}⚠ ImageMagick not found. Copied original icon.${NC}"
        echo -e "${YELLOW}  Please manually resize to 512x512 if needed.${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Launcher icon not found, skipping...${NC}"
fi

echo ""

# Copy phone screenshots
echo -e "${BLUE}Copying phone screenshots...${NC}"

PHONE_SOURCE="$SCREENSHOTS_BASE/phone"
PHONE_DEST="$FASTLANE_BASE/en-US/images/phoneScreenshots"

if [ -d "$PHONE_SOURCE" ]; then
    counter=1
    for screenshot in "$PHONE_SOURCE"/phone_*_portrait.png; do
        if [ -f "$screenshot" ]; then
            cp "$screenshot" "$PHONE_DEST/$counter.png"
            echo "  ✓ Copied $(basename "$screenshot") → $counter.png"
            ((counter++))
        fi
    done
    
    if [ $counter -eq 1 ]; then
        for screenshot in "$PHONE_SOURCE"/phone_*.png; do
            if [[ "$screenshot" != *"_portrait.png" ]]; then
                cp "$screenshot" "$PHONE_DEST/$counter.png"
                echo "  ✓ Copied $(basename "$screenshot") → $counter.png"
                ((counter++))
            fi
        done
    fi
    
    echo -e "${GREEN}✓ Copied $((counter-1)) phone screenshots${NC}"
else
    echo -e "${YELLOW}⚠ Phone screenshots directory not found${NC}"
fi

echo ""

# Copy tablet screenshots
echo -e "${BLUE}Copying tablet screenshots...${NC}"

TABLET7_SOURCE="$SCREENSHOTS_BASE/tablet7"
TABLET7_DEST="$FASTLANE_BASE/en-US/images/sevenInchScreenshots"

if [ -d "$TABLET7_SOURCE" ]; then
    counter=1
    for screenshot in "$TABLET7_SOURCE"/tablet7_*_portrait.png; do
        if [ -f "$screenshot" ]; then
            cp "$screenshot" "$TABLET7_DEST/$counter.png"
            ((counter++))
        fi
    done
    echo -e "${GREEN}✓ Copied $((counter-1)) 7-inch tablet screenshots${NC}"
fi

TABLET10_SOURCE="$SCREENSHOTS_BASE/tablet10"
TABLET10_DEST="$FASTLANE_BASE/en-US/images/tenInchScreenshots"

if [ -d "$TABLET10_SOURCE" ]; then
    counter=1
    for screenshot in "$TABLET10_SOURCE"/tablet10_*_portrait.png; do
        if [ -f "$screenshot" ]; then
            cp "$screenshot" "$TABLET10_DEST/$counter.png"
            ((counter++))
        fi
    done
    echo -e "${GREEN}✓ Copied $((counter-1)) 10-inch tablet screenshots${NC}"
fi

echo ""

# Create feature graphic
echo -e "${BLUE}Creating feature graphic...${NC}"

FEATURE_GRAPHIC="$FASTLANE_BASE/en-US/images/featureGraphic.png"

if [ -f "$PHONE_SOURCE/phone_001_portrait.png" ] && command -v convert &> /dev/null; then
    convert "$PHONE_SOURCE/phone_001_portrait.png" \
        -resize 1024x500! \
        -gravity center \
        -background '#6200EE' \
        -extent 1024x500 \
        "$FEATURE_GRAPHIC"
    echo -e "${GREEN}✓ Feature graphic created${NC}"
    echo -e "${YELLOW}  Note: This is auto-generated. Consider creating a custom graphic.${NC}"
else
    echo -e "${YELLOW}⚠ Could not create feature graphic automatically${NC}"
fi

echo ""

# Create metadata files
echo -e "${BLUE}Creating metadata files...${NC}"

cat > "$FASTLANE_BASE/en-US/title.txt" << 'EOF'
Àpò - Monero Wallet
EOF

cat > "$FASTLANE_BASE/en-US/short_description.txt" << 'EOF'
Privacy-focused Monero wallet with modern Material Design 3 interface
EOF

cat > "$FASTLANE_BASE/en-US/full_description.txt" << 'EOF'
Àpò is a native Monero wallet application for Android built with modern technologies including Kotlin and Jetpack Compose, offering a secure and user-friendly way to manage your XMR cryptocurrency.

<b>Key Features</b>

• Create new Monero wallets or restore existing ones using seed phrases
• Send and receive XMR transactions with ease
• Real-time blockchain synchronization
• Complete transaction history with powerful search functionality
• Blockchain rescan capability for wallet recovery and troubleshooting
• QR code generation and scanning for seamless address sharing
• Biometric authentication (fingerprint/face unlock) for enhanced security
• Modern, intuitive Material Design 3 interface
• Multi-language support

<b>Privacy & Security</b>

Àpò prioritizes your privacy and security:

• Native Monero C++ libraries ensure protocol compatibility
• All wallet data encrypted and stored locally on your device
• No analytics or tracking - your financial data stays private
• Biometric authentication protects wallet access
• Open source - auditable by anyone

<b>Technology</b>

Built with cutting-edge Android development tools:

• Kotlin programming language for reliability
• Jetpack Compose for smooth, modern UI
• MVVM architecture with coroutines for responsive performance
• Room database for efficient local data management
• Native Monero libraries via JNI for secure wallet operations

<b>Important Security Note</b>

Always backup your seed phrase securely. Never share your seed phrase or private keys with anyone. Àpò developers will never ask for your seed phrase or private keys.

<b>About Monero</b>

Monero (XMR) is a privacy-focused cryptocurrency that uses ring signatures, stealth addresses, and confidential transactions to keep your financial information private.
EOF

cat > "$FASTLANE_BASE/en-US/changelogs/31.txt" << 'EOF'
Version 0.0.31

• Initial F-Droid release
• Native Monero wallet functionality
• Material Design 3 interface
• Biometric authentication support
• QR code scanning and generation
• Transaction history with search
• Multi-language support
• Enhanced security features
EOF

echo -e "${GREEN}✓ Metadata files created${NC}"
echo ""

PHONE_COUNT=$(ls -1 "$FASTLANE_BASE/en-US/images/phoneScreenshots" 2>/dev/null | wc -l)

echo -e "${GREEN}=========================================="
echo "Graphics Organization Complete!"
echo "==========================================${NC}"
echo ""
echo "Summary:"
echo "  Phone screenshots: $PHONE_COUNT"
echo "  Location: $FASTLANE_BASE/en-US/"
echo ""
echo "Next: Review the files and run complete-fdroid-setup.sh"
echo ""
SCRIPT_EOF

chmod +x organize-fdroid-graphics.sh
echo "✓ Created organize-fdroid-graphics.sh"

# 2. prepare-fdroid.sh (scripts/)
cat > scripts/prepare-fdroid.sh << 'SCRIPT_EOF'
#!/bin/bash

# prepare-fdroid.sh
# Prepares build.gradle.kts for F-Droid compatibility

set -e

echo "Preparing F-Droid build configuration..."

BUILD_GRADLE="app/build.gradle.kts"

if [ ! -f "$BUILD_GRADLE" ]; then
    echo "Error: $BUILD_GRADLE not found"
    exit 1
fi

# Backup original
cp "$BUILD_GRADLE" "$BUILD_GRADLE.backup"

# Check if flavor already exists
if grep -q "flavorDimensions.*distribution" "$BUILD_GRADLE"; then
    echo "✓ F-Droid flavor already configured"
else
    echo "Adding F-Droid build flavors..."
    # Note: This is a placeholder - the actual modification should be done manually
    # or with the updated build.gradle.kts provided earlier
    echo "⚠ Please apply the build.gradle.kts changes from the documentation"
fi

# Create .fdroidignore
cat > .fdroidignore << 'EOF'
# Proprietary files to exclude from F-Droid builds
google-services.json
*.keystore
*.jks
key.properties
local.properties
EOF

echo "✓ Created .fdroidignore"

# Create FDROID.md
cat > FDROID.md << 'EOF'
# F-Droid Build Instructions

This is the F-Droid compatible version of Àpò.

## Building

```bash
./gradlew assembleFdroidRelease
```

## Differences from Play Store version

- No Firebase Analytics or Crashlytics
- Uses ZXing instead of ML Kit for barcode scanning
- No proprietary Google dependencies

## Testing

```bash
./gradlew assembleFdroidDebug
adb install app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
```
EOF

echo "✓ Created FDROID.md"

echo "✓ F-Droid preparation complete"
SCRIPT_EOF

chmod +x scripts/prepare-fdroid.sh
echo "✓ Created scripts/prepare-fdroid.sh"

# 3. cleanup-fdroid.sh (scripts/)
cat > scripts/cleanup-fdroid.sh << 'SCRIPT_EOF'
#!/bin/bash

# cleanup-fdroid.sh
# Removes proprietary files from F-Droid branch

set -e

echo "Cleaning up proprietary files..."

# Remove Google Services JSON if it exists
if [ -f "app/google-services.json" ]; then
    git rm -f app/google-services.json 2>/dev/null || rm -f app/google-services.json
    echo "✓ Removed google-services.json"
fi

# Remove keystore files
find . -name "*.keystore" -o -name "*.jks" | while read file; do
    git rm -f "$file" 2>/dev/null || rm -f "$file"
    echo "✓ Removed $file"
done

# Remove key.properties if exists
if [ -f "key.properties" ]; then
    git rm -f key.properties 2>/dev/null || rm -f key.properties
    echo "✓ Removed key.properties"
fi

echo "✓ Cleanup complete"
SCRIPT_EOF

chmod +x scripts/cleanup-fdroid.sh
echo "✓ Created scripts/cleanup-fdroid.sh"

# 4. validate-fdroid.sh (scripts/)
cat > scripts/validate-fdroid.sh << 'SCRIPT_EOF'
#!/bin/bash

# validate-fdroid.sh
# Validates F-Droid requirements

set -e

echo "=========================================="
echo "F-Droid Validation"
echo "=========================================="
echo ""

errors=0
warnings=0

# Check build.gradle.kts
if grep -q "flavorDimensions.*distribution" app/build.gradle.kts; then
    echo "✓ Build flavors configured"
else
    echo "✗ Build flavors not found"
    ((errors++))
fi

# Check for proprietary dependencies
if grep -rq "com.google.firebase" app/build.gradle.kts; then
    if grep -q "playstoreImplementation.*firebase" app/build.gradle.kts; then
        echo "✓ Firebase only in playstore flavor"
    else
        echo "✗ Firebase in main dependencies"
        ((errors++))
    fi
fi

# Check Fastlane structure
if [ -d "fastlane/metadata/android/en-US" ]; then
    echo "✓ Fastlane metadata structure exists"
else
    echo "✗ Fastlane metadata structure missing"
    ((errors++))
fi

# Check screenshots
screenshot_count=$(ls -1 fastlane/metadata/android/en-US/images/phoneScreenshots 2>/dev/null | wc -l)
if [ "$screenshot_count" -ge 2 ]; then
    echo "✓ Screenshots present ($screenshot_count)"
else
    echo "✗ Insufficient screenshots ($screenshot_count, need at least 2)"
    ((errors++))
fi

# Check icon
if [ -f "fastlane/metadata/android/en-US/images/icon.png" ]; then
    echo "✓ Icon present"
else
    echo "✗ Icon missing"
    ((errors++))
fi

# Check metadata files
for file in title.txt short_description.txt full_description.txt; do
    if [ -f "fastlane/metadata/android/en-US/$file" ]; then
        echo "✓ $file present"
    else
        echo "✗ $file missing"
        ((errors++))
    fi
done

# Check LICENSE
if [ -f "LICENSE" ]; then
    echo "✓ LICENSE file present"
else
    echo "⚠ LICENSE file missing"
    ((warnings++))
fi

echo ""
echo "=========================================="
echo "Validation Summary"
echo "=========================================="
echo "Errors: $errors"
echo "Warnings: $warnings"

if [ $errors -eq 0 ]; then
    echo ""
    echo "✓ All checks passed!"
    exit 0
else
    echo ""
    echo "✗ Validation failed"
    exit 1
fi
SCRIPT_EOF

chmod +x scripts/validate-fdroid.sh
echo "✓ Created scripts/validate-fdroid.sh"

# 5. complete-fdroid-setup.sh (root level)
cat > complete-fdroid-setup.sh << 'SCRIPT_EOF'
#!/bin/bash

# complete-fdroid-setup.sh
# Complete F-Droid setup including branch creation, graphics, and metadata

set -e

echo "=========================================="
echo "Complete F-Droid Setup Script for Àpò"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "Error: Not in a git repository"
    exit 1
fi

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
FDROID_BRANCH="fdroid"

echo -e "${CYAN}Step 1/5: Creating F-Droid branch${NC}"
echo "----------------------------------------"

if git show-ref --verify --quiet refs/heads/$FDROID_BRANCH; then
    echo -e "${YELLOW}Branch '$FDROID_BRANCH' already exists.${NC}"
    git checkout $FDROID_BRANCH
else
    git checkout -b $FDROID_BRANCH
    echo -e "${GREEN}✓ Created new branch: $FDROID_BRANCH${NC}"
fi

echo ""
echo -e "${CYAN}Step 2/5: Organizing graphics${NC}"
echo "----------------------------------------"
./organize-fdroid-graphics.sh

echo ""
echo -e "${CYAN}Step 3/5: Preparing build configuration${NC}"
echo "----------------------------------------"
./scripts/prepare-fdroid.sh

echo ""
echo -e "${CYAN}Step 4/5: Cleaning proprietary files${NC}"
echo "----------------------------------------"
./scripts/cleanup-fdroid.sh

echo ""
echo -e "${CYAN}Step 5/5: Validating${NC}"
echo "----------------------------------------"
./scripts/validate-fdroid.sh || echo "Some validations failed - review above"

echo ""
echo -e "${GREEN}=========================================="
echo "Setup Complete!"
echo "==========================================${NC}"
echo ""
echo "Next steps:"
echo "1. Review changes: git status"
echo "2. Update build.gradle.kts with F-Droid flavors (see documentation)"
echo "3. Test build: ./scripts/test-fdroid-build.sh"
echo "4. Commit: git add . && git commit -m 'Prepare F-Droid branch'"
echo "5. Push: git push -u origin $FDROID_BRANCH"
echo ""
SCRIPT_EOF

chmod +x complete-fdroid-setup.sh
echo "✓ Created complete-fdroid-setup.sh"

echo ""
echo "=========================================="
echo "All Scripts Generated!"
echo "=========================================="
echo ""
echo "Created:"
echo "  ✓ organize-fdroid-graphics.sh"
echo "  ✓ complete-fdroid-setup.sh"
echo "  ✓ scripts/prepare-fdroid.sh"
echo "  ✓ scripts/cleanup-fdroid.sh"
echo "  ✓ scripts/validate-fdroid.sh"
echo ""
echo "To start F-Droid setup, run:"
echo "  ./complete-fdroid-setup.sh"
echo ""
SCRIPT_EOF

chmod +x generate-fdroid-scripts.sh
echo "✓ Created generate-fdroid-scripts.sh"
