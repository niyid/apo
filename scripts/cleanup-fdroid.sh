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
