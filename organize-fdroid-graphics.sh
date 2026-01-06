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
