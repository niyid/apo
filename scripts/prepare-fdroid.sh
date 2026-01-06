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
