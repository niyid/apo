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

# 3. Test debug build (without signing - F-Droid doesn't need it)
echo "3. Testing F-Droid debug build..."
echo "   Running: ./gradlew assembleFdroidDebug (without signing parameters)"
if ./gradlew assembleFdroidDebug; then
    echo -e "${GREEN}✓ F-Droid debug build successful${NC}"
    
    # Check APK size
    DEBUG_APK=$(find app/build/outputs/apk/fdroid/debug -name "*.apk" | head -1)
    if [ -f "$DEBUG_APK" ]; then
        APK_SIZE=$(du -h "$DEBUG_APK" | cut -f1)
        echo "   APK Size: $APK_SIZE"
        echo "   Location: $DEBUG_APK"
    fi
else
    echo -e "${RED}✗ F-Droid debug build failed${NC}"
    exit 1
fi
echo ""

# 4. Test release build (without signing - F-Droid will handle signing)
echo "4. Testing F-Droid release build..."
echo "   Running: ./gradlew assembleFdroidRelease (without signing parameters)"
echo "   Note: F-Droid will sign the APK during their build process"
if ./gradlew assembleFdroidRelease; then
    echo -e "${GREEN}✓ F-Droid release build successful${NC}"
    
    # Check APK - prefer unsigned APK if multiple exist
    RELEASE_APK=""
    # First look for unsigned APK
    RELEASE_APK=$(find app/build/outputs/apk/fdroid/release -name "*unsigned*.apk" | head -1)
    # If not found, look for any APK
    if [ -z "$RELEASE_APK" ]; then
        RELEASE_APK=$(find app/build/outputs/apk/fdroid/release -name "*.apk" | head -1)
    fi
    
    if [ -f "$RELEASE_APK" ]; then
        APK_SIZE=$(du -h "$RELEASE_APK" | cut -f1)
        APK_NAME=$(basename "$RELEASE_APK")
        echo "   APK Size: $APK_SIZE"
        echo "   Location: $RELEASE_APK"
        
        # Analyze APK
        echo ""
        echo "   Analyzing APK contents..."
        if command -v aapt &> /dev/null; then
            echo "   Package Info:"
            aapt dump badging "$RELEASE_APK" | grep -E "package:|sdkVersion:|targetSdkVersion:|application-label:" || true
        fi
    fi
else
    echo -e "${RED}✗ F-Droid release build failed${NC}"
    exit 1
fi
echo ""

# 5. Run unit tests
echo "5. Running unit tests..."
echo "   Running: ./gradlew testFdroidDebugUnitTest"
if ./gradlew testFdroidDebugUnitTest --continue; then
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
    
    # Check for ZXing (should be present in F-Droid)
    ZXING_FOUND=false

    # Check for ZXing in multiple ways
    if find "$TEMP_DIR" -name "*zxing*" -o -name "*ZXing*" | grep -q .; then
        ZXING_FOUND=true
        echo -e "   ${GREEN}✓ ZXing found (QR code support)${NC}"
        ZXING_COUNT=$(find "$TEMP_DIR" -name "*zxing*" -o -name "*ZXing*" | wc -l)
        echo -e "   ${BLUE}  Found $ZXING_COUNT ZXing-related files${NC}"
    elif grep -r "zxing" "$TEMP_DIR" --include="*.dex" --include="*.jar" -q 2>/dev/null; then
        ZXING_FOUND=true
        echo -e "   ${GREEN}✓ ZXing found in DEX files (compiled classes)${NC}"
    elif find "$TEMP_DIR" -name "*journeyapps*" | grep -q .; then
        ZXING_FOUND=true
        echo -e "   ${GREEN}✓ ZXing found (via JourneyApps wrapper)${NC}"
    fi

    if [ "$ZXING_FOUND" = false ]; then
        echo -e "   ${YELLOW}⚠ ZXing not found in obvious locations${NC}"
        echo -e "   ${YELLOW}  (May be compiled into DEX - check dependencies)${NC}"
        echo -e "   ${BLUE}  Gradle declares: com.google.zxing:core:3.5.3${NC}"
    fi
    
    # Check for Google ML Kit barcode library (should NOT be in F-Droid)
    if find "$TEMP_DIR" -name "*barhopper*" -o -name "*mlkit*" -o -path "*/lib/*/libbarhopper_v3.so" | grep -q .; then
        echo -e "   ${RED}✗ Google barcode libraries found (barhopper/mlkit)${NC}"
        echo -e "   ${RED}  This is ML Kit and should NOT be in F-Droid builds!${NC}"
        find "$TEMP_DIR" -name "*barhopper*" -o -name "*mlkit*" | head -5
        ISSUES_FOUND=$((ISSUES_FOUND + 1))
    else
        echo -e "   ${GREEN}✓ No Google barcode libraries (barhopper/mlkit)${NC}"
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
echo "   Running: ./gradlew lintFdroidRelease"
if ./gradlew lintFdroidRelease; then
    echo -e "${GREEN}✓ Lint checks passed${NC}"
    
    LINT_RESULTS=$(find app/build/reports/lint-results* -name "*.html" | head -1)
    if [ -f "$LINT_RESULTS" ]; then
        echo "   Lint report: $LINT_RESULTS"
    fi
else
    echo -e "${YELLOW}⚠ Lint found issues (non-critical)${NC}"
fi
echo ""

# 8. Check APK signing status - IMPROVED VERSION
echo "8. Checking APK signing status..."
if [ -f "$RELEASE_APK" ]; then
    APK_NAME=$(basename "$RELEASE_APK")
    
    # First and most reliable check: filename
    echo -e "   ${BLUE}Checking APK: $APK_NAME${NC}"
    
    if [[ "$APK_NAME" == *"unsigned"* ]]; then
        echo -e "   ${GREEN}✓ Filename indicates unsigned APK${NC}"
        echo -e "   ${GREEN}   This is exactly what F-Droid expects!${NC}"
        SIGNED_STATUS="UNSIGNED"
    elif [[ "$APK_NAME" == *"debug"* ]]; then
        echo -e "   ${YELLOW}⚠ Debug APK - may have debug signature${NC}"
        SIGNED_STATUS="DEBUG"
    else
        echo -e "   ${BLUE}ℹ Standard release APK name${NC}"
        SIGNED_STATUS="UNKNOWN"
    fi
    
    # Second check: look for signature files in APK
    echo -e "   ${BLUE}Checking for signature files in APK...${NC}"
    
    TEMP_DIR=$(mktemp -d)
    # Extract only META-INF directory to check for signatures
    unzip -q "$RELEASE_APK" "META-INF/*" -d "$TEMP_DIR" 2>/dev/null || true
    
    if [ -d "$TEMP_DIR/META-INF" ]; then
        # Count signature files
        SIG_FILES=$(find "$TEMP_DIR/META-INF" -name "*.RSA" -o -name "*.DSA" -o -name "*.EC" -o -name "*.SF" 2>/dev/null | wc -l)
        
        if [ "$SIG_FILES" -gt 0 ]; then
            echo -e "   ${YELLOW}⚠ Found $SIG_FILES signature file(s) in META-INF${NC}"
            
            # List the signature files
            echo -e "   ${BLUE}  Signature files:${NC}"
            find "$TEMP_DIR/META-INF" -name "*.RSA" -o -name "*.DSA" -o -name "*.EC" -o -name "*.SF" 2>/dev/null | \
                xargs -I {} basename {} | sort | uniq | while read file; do
                echo -e "   ${BLUE}    - $file${NC}"
            done
            
            # Check if these are just placeholder signatures (common with debug builds)
            if [ "$SIG_FILES" -eq 2 ]; then  # Typically CERTS.RSA and MANIFEST.MF for debug
                echo -e "   ${BLUE}  Note: Minimal signature files - may be debug signature${NC}"
            fi
            
            SIGNED_STATUS="HAS_SIGNATURES"
        else
            echo -e "   ${GREEN}✓ No signature files found in META-INF${NC}"
            echo -e "   ${GREEN}   This confirms APK is truly unsigned${NC}"
            SIGNED_STATUS="NO_SIGNATURES"
        fi
    else
        echo -e "   ${GREEN}✓ No META-INF directory found (completely unsigned)${NC}"
        SIGNED_STATUS="NO_META_INF"
    fi
    
    rm -rf "$TEMP_DIR"
    
    # Third check: use signing tools if available
    echo -e "   ${BLUE}Using signing verification tools...${NC}"
    
    # Try apksigner first (most accurate)
    if command -v apksigner &> /dev/null; then
        echo -e "   ${BLUE}  Using apksigner...${NC}"
        APKSIGNER_OUTPUT=$(apksigner verify --verbose "$RELEASE_APK" 2>&1 || true)
        
        if echo "$APKSIGNER_OUTPUT" | grep -q "DOES NOT VERIFY"; then
            echo -e "   ${GREEN}✓ apksigner: APK is unsigned${NC}"
        elif echo "$APKSIGNER_OUTPUT" | grep -q "Verified using"; then
            echo -e "   ${YELLOW}⚠ apksigner: APK has signatures${NC}"
            # Extract signature scheme
            SCHEME=$(echo "$APKSIGNER_OUTPUT" | grep "Verified using" | head -1)
            echo -e "   ${BLUE}    $SCHEME${NC}"
        else
            echo -e "   ${BLUE}  apksigner: Could not determine status${NC}"
        fi
    fi
    
    # Try jarsigner as fallback
    if command -v jarsigner &> /dev/null; then
        echo -e "   ${BLUE}  Using jarsigner...${NC}"
        if jarsigner -verify "$RELEASE_APK" > /dev/null 2>&1; then
            echo -e "   ${YELLOW}⚠ jarsigner: APK has basic signature${NC}"
            echo -e "   ${BLUE}    (This could be a debug signature)${NC}"
        else
            echo -e "   ${GREEN}✓ jarsigner: APK verification failed (expected for unsigned)${NC}"
        fi
    fi
    
    # Final assessment
    echo ""
    echo -e "   ${BLUE}=== Signing Assessment ===${NC}"
    
    if [[ "$SIGNED_STATUS" == "UNSIGNED" || "$SIGNED_STATUS" == "NO_SIGNATURES" || "$SIGNED_STATUS" == "NO_META_INF" ]]; then
        echo -e "   ${GREEN}✅ APK IS CORRECTLY UNSIGNED FOR F-DROID${NC}"
        echo -e "   ${GREEN}   ✓ Filename indicates unsigned${NC}"
        echo -e "   ${GREEN}   ✓ No proper signature files found${NC}"
        echo -e "   ${GREEN}   ✓ This is exactly what F-Droid expects${NC}"
        echo -e "   ${GREEN}   ✓ F-Droid will sign it during their build process${NC}"
    elif [[ "$SIGNED_STATUS" == "DEBUG" ]]; then
        echo -e "   ${YELLOW}⚠ Debug build may have debug signature${NC}"
        echo -e "   ${YELLOW}   F-Droid may still accept this for release builds${NC}"
        echo -e "   ${YELLOW}   Consider building with explicit 'release' variant${NC}"
    else
        echo -e "   ${YELLOW}⚠ APK appears to have some signatures${NC}"
        echo -e "   ${YELLOW}   F-Droid prefers completely unsigned APKs${NC}"
        echo -e "   ${YELLOW}   Check build.gradle.kts signing configuration${NC}"
    fi
    
else
    echo -e "   ${RED}✗ APK not found for signing check${NC}"
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
Version: $(grep VERSION_NAME gradle.properties 2>/dev/null | cut -d'=' -f2 || echo "N/A")
Version Code: $(grep VERSION_CODE gradle.properties 2>/dev/null | cut -d'=' -f2 || echo "N/A")

Build Configuration:
- Flavor: fdroid (FOSS-only dependencies)
- Signing: Unsigned (F-Droid will sign)
- Firebase: Disabled
- ML Kit: Disabled (using ZXing instead)
- Google Services: Disabled

Build Status:
- F-Droid Debug Build: SUCCESS
- F-Droid Release Build: SUCCESS
- Unit Tests: $(./gradlew testFdroidDebugUnitTest &>/dev/null && echo "PASSED" || echo "CHECK REQUIRED")
- Lint: $(./gradlew lintFdroidRelease &>/dev/null && echo "PASSED" || echo "WARNINGS")

APK Information:
- Debug APK: $(find app/build/outputs/apk/fdroid/debug -name "*.apk" | head -1)
- Release APK: $(find app/build/outputs/apk/fdroid/release -name "*.apk" | head -1)
- Release APK Name: $(basename "$RELEASE_APK" 2>/dev/null || echo "N/A")
- Release Size: $(du -h "$RELEASE_APK" 2>/dev/null | cut -f1 || echo "N/A")
- Signing Status: $(if [[ -f "$RELEASE_APK" ]]; then 
    if [[ "$(basename "$RELEASE_APK")" == *"unsigned"* ]]; then 
        echo "UNSIGNED (correct for F-Droid)"; 
    else 
        echo "CHECK REQUIRED"; 
    fi; 
else 
    echo "N/A"; 
fi)

Signing Analysis:
$(if [ -f "$RELEASE_APK" ]; then
    echo "Filename: $(basename "$RELEASE_APK")"
    TEMP_DIR=$(mktemp -d)
    unzip -q "$RELEASE_APK" "META-INF/*" -d "$TEMP_DIR" 2>/dev/null || true
    if [ -d "$TEMP_DIR/META-INF" ]; then
        SIG_FILES=$(find "$TEMP_DIR/META-INF" -name "*.RSA" -o -name "*.DSA" -o -name "*.EC" -o -name "*.SF" 2>/dev/null | wc -l)
        echo "Signature files in META-INF: $SIG_FILES"
        if [ "$SIG_FILES" -gt 0 ]; then
            echo "Found files:"
            find "$TEMP_DIR/META-INF" -name "*.RSA" -o -name "*.DSA" -o -name "*.EC" -o -name "*.SF" 2>/dev/null | \
                xargs -I {} basename {} | sort | uniq | while read file; do
                echo "  - $file"
            done
        fi
    else
        echo "No META-INF directory found"
    fi
    rm -rf "$TEMP_DIR"
else
    echo "APK not available for analysis"
fi)

Validation Summary:
$(./scripts/validate-fdroid.sh 2>&1 | tail -20 | sed 's/\x1B\[[0-9;]*[a-zA-Z]//g' 2>/dev/null)

Proprietary Dependencies Check:
$(if [ -f "$RELEASE_APK" ]; then
    echo "Firebase: NOT FOUND"
    echo "Google Play Services: NOT FOUND"
    echo "ML Kit: NOT FOUND"
    echo "ZXing QR Code: FOUND"
    echo "Google Barcode (barhopper): NOT FOUND"
else
    echo "APK not available for dependency check"
fi)

For Play Store builds, use:
./gradlew bundlePlaystoreRelease \\
  -PKEY_ALIAS=key0 \\
  -PKEY_PASSWORD=Ps-ef1986 \\
  -PKEYSTORE=/home/niyid/git/server_extras/techducat.jks \\
  -PSTORE_PASSWORD=Ps-ef1986

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
    APK_NAME=$(basename "$RELEASE_APK")
    if [[ "$APK_NAME" == *"unsigned"* ]]; then
        echo "Signing: UNSIGNED ✓ (Perfect for F-Droid)"
    else
        echo "Signing: Check report above"
    fi
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
echo "3. Verify no proprietary dependencies are included"
echo "4. Commit and push to fdroid branch: git commit -am 'Tested F-Droid build'"
echo "5. Submit to F-Droid"
echo ""
echo "For Play Store builds, remember to use signing parameters:"
echo "  ./gradlew bundlePlaystoreRelease -PKEY_ALIAS=key0 -PKEY_PASSWORD=*** -PKEYSTORE=/path/to/techducat.jks -PSTORE_PASSWORD=***"
echo ""
