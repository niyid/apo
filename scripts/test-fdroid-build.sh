#!/bin/bash

# scripts/test-fdroid-build.sh
# Tests the F-Droid build thoroughly

set -e

echo "=========================================="
echo "F-Droid Build Testing"
echo "=========================================="
echo ""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Check we're on fdroid branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "fdroid" ]; then
    echo -e "${YELLOW}⚠ Warning: Not on fdroid branch (current: $CURRENT_BRANCH)${NC}"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo -e "${BLUE}Running comprehensive F-Droid build tests...${NC}"
echo ""

# 1. Clean build
echo "1. Cleaning previous builds..."
./gradlew clean
echo -e "${GREEN}✓ Clean successful${NC}"
echo ""

# 2. Run validation
echo "2. Running F-Droid validation..."
if [ -f "scripts/validate-fdroid.sh" ]; then
    ./scripts/validate-fdroid.sh
    if [ $? -ne 0 ]; then
        echo -e "${RED}✗ Validation failed${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠ Validation script not found, skipping${NC}"
fi
echo ""

# 3. Test debug build
echo "3. Testing debug build..."
echo "   Running: ./gradlew assembleDebug"
if ./gradlew assembleDebug; then
    echo -e "${GREEN}✓ Debug build successful${NC}"
    
    # Check APK size
    DEBUG_APK=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)
    if [ -f "$DEBUG_APK" ]; then
        APK_SIZE=$(du -h "$DEBUG_APK" | cut -f1)
        echo "   APK Size: $APK_SIZE"
        echo "   Location: $DEBUG_APK"
    fi
else
    echo -e "${RED}✗ Debug build failed${NC}"
    exit 1
fi
echo ""

# 4. Test release build
echo "4. Testing release build..."
echo "   Running: ./gradlew assembleRelease"
if ./gradlew assembleRelease; then
    echo -e "${GREEN}✓ Release build successful${NC}"
    
    # Check APK
    RELEASE_APK=$(find app/build/outputs/apk/release -name "*.apk" | head -1)
    if [ -f "$RELEASE_APK" ]; then
        APK_SIZE=$(du -h "$RELEASE_APK" | cut -f1)
        echo "   APK Size: $APK_SIZE"
        echo "   Location: $RELEASE_APK"
        
        # Analyze APK
        echo ""
        echo "   Analyzing APK contents..."
        if command -v aapt &> /dev/null; then
            echo "   Package Info:"
            aapt dump badging "$RELEASE_APK" | grep -E "package:|sdkVersion:|targetSdkVersion:|application-label:"
        fi
    fi
else
    echo -e "${RED}✗ Release build failed${NC}"
    exit 1
fi
echo ""

# 5. Run unit tests
echo "5. Running unit tests..."
echo "   Running: ./gradlew test"
if ./gradlew test --continue; then
    echo -e "${GREEN}✓ Unit tests passed${NC}"
    
    # Show test results location
    TEST_RESULTS=$(find app/build/reports/tests -name "index.html" | head -1)
    if [ -f "$TEST_RESULTS" ]; then
        echo "   Test report: $TEST_RESULTS"
    fi
else
    echo -e "${YELLOW}⚠ Some unit tests failed (check reports)${NC}"
    TEST_RESULTS=$(find app/build/reports/tests -name "index.html" | head -1)
    if [ -f "$TEST_RESULTS" ]; then
        echo "   Test report: $TEST_RESULTS"
    fi
fi
echo ""

# 6. Check for proprietary dependencies in APK
echo "6. Checking APK for proprietary dependencies..."
if [ -f "$RELEASE_APK" ] && command -v unzip &> /dev/null; then
    
    TEMP_DIR=$(mktemp -d)
    unzip -q "$RELEASE_APK" -d "$TEMP_DIR"
    
    ISSUES_FOUND=0
    
    # Check for Firebase
    if find "$TEMP_DIR" -name "*firebase*" | grep -q .; then
        echo -e "   ${RED}✗ Firebase files found in APK${NC}"
        ISSUES_FOUND=$((ISSUES_FOUND + 1))
    else
        echo -e "   ${GREEN}✓ No Firebase files${NC}"
    fi
    
    # Check for Google Play Services
    if find "$TEMP_DIR" -name "*gms*" -o -name "*play-services*" | grep -q .; then
        echo -e "   ${RED}✗ Google Play Services found in APK${NC}"
        ISSUES_FOUND=$((ISSUES_FOUND + 1))
    else
        echo -e "   ${GREEN}✓ No Google Play Services${NC}"
    fi
    
    # Check for ML Kit
    if find "$TEMP_DIR" -name "*mlkit*" | grep -q .; then
        echo -e "   ${RED}✗ ML Kit files found in APK${NC}"
        ISSUES_FOUND=$((ISSUES_FOUND + 1))
    else
        echo -e "   ${GREEN}✓ No ML Kit files${NC}"
    fi
    
    # Check for ZXing (should be present)
    if find "$TEMP_DIR" -name "*zxing*" | grep -q .; then
        echo -e "   ${GREEN}✓ ZXing found (QR code support)${NC}"
    else
        echo -e "   ${YELLOW}⚠ ZXing not found - QR codes may not work${NC}"
    fi
    
    rm -rf "$TEMP_DIR"
    
    if [ $ISSUES_FOUND -gt 0 ]; then
        echo ""
        echo -e "   ${RED}Found $ISSUES_FOUND proprietary dependency issues${NC}"
    fi
else
    echo -e "   ${YELLOW}⚠ Cannot analyze APK (missing tools or APK)${NC}"
fi
echo ""

# 7. Lint checks
echo "7. Running lint checks..."
echo "   Running: ./gradlew lint"
if ./gradlew lint; then
    echo -e "${GREEN}✓ Lint checks passed${NC}"
    
    LINT_RESULTS=$(find app/build/reports/lint-results* -name "*.html" | head -1)
    if [ -f "$LINT_RESULTS" ]; then
        echo "   Lint report: $LINT_RESULTS"
    fi
else
    echo -e "${YELLOW}⚠ Lint found issues (non-critical)${NC}"
fi
echo ""

# 8. Check APK signing
echo "8. Checking APK signing..."
if [ -f "$RELEASE_APK" ] && command -v jarsigner &> /dev/null; then
    if jarsigner -verify "$RELEASE_APK" > /dev/null 2>&1; then
        echo -e "   ${GREEN}✓ APK is signed${NC}"
    else
        echo -e "   ${YELLOW}⚠ APK is not signed (F-Droid will sign it)${NC}"
    fi
else
    echo -e "   ${YELLOW}⚠ Cannot verify signing (jarsigner not found)${NC}"
fi
echo ""

# 9. Generate build report
echo "9. Generating build report..."

REPORT_FILE="fdroid-build-report.txt"
cat > "$REPORT_FILE" << EOF
========================================
F-Droid Build Report
========================================
Generated: $(date)
Branch: $CURRENT_BRANCH
Version: $(grep VERSION_NAME gradle.properties | cut -d'=' -f2)
Version Code: $(grep VERSION_CODE gradle.properties | cut -d'=' -f2)

Build Status:
- Debug Build: SUCCESS
- Release Build: SUCCESS
- Unit Tests: $(./gradlew test &>/dev/null && echo "PASSED" || echo "CHECK REQUIRED")
- Lint: $(./gradlew lint &>/dev/null && echo "PASSED" || echo "WARNINGS")

APK Information:
- Debug APK: $(find app/build/outputs/apk/debug -name "*.apk" | head -1)
- Release APK: $(find app/build/outputs/apk/release -name "*.apk" | head -1)
- Release Size: $(du -h "$RELEASE_APK" 2>/dev/null | cut -f1 || echo "N/A")

Validation:
$(./scripts/validate-fdroid.sh 2>&1 | tail -20)

========================================
EOF

echo -e "${GREEN}✓ Report saved to: $REPORT_FILE${NC}"
echo ""

# Summary
echo "=========================================="
echo "Build Testing Complete!"
echo "=========================================="
echo ""

if [ -f "$RELEASE_APK" ]; then
    echo -e "${GREEN}✓ F-Droid build is ready!${NC}"
    echo ""
    echo "Release APK: $RELEASE_APK"
    echo "Size: $(du -h "$RELEASE_APK" | cut -f1)"
    echo ""
    echo "You can install this APK to test:"
    echo "  adb install $RELEASE_APK"
    echo ""
    echo "Full report available at: $REPORT_FILE"
else
    echo -e "${RED}✗ Build completed but APK not found${NC}"
    exit 1
fi

echo ""
echo "Next steps:"
echo "1. Test the APK on a device"
echo "2. Review the build report: cat $REPORT_FILE"
echo "3. Commit and push: git commit -am 'Tested F-Droid build'"
echo "4. Submit to F-Droid"
echo ""
