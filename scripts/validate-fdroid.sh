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
