#!/bin/bash

# Script to add missing string resources to strings.xml

STRINGS_FILE="app/src/main/res/values/strings.xml"

# Check if strings.xml exists
if [ ! -f "$STRINGS_FILE" ]; then
    echo "Error: $STRINGS_FILE not found!"
    exit 1
fi

# Create backup
cp "$STRINGS_FILE" "${STRINGS_FILE}.backup"
echo "Created backup: ${STRINGS_FILE}.backup"

# Missing strings to add
MISSING_STRINGS='
    <!-- Pocket Change -->
    <string name="pocketchange_title">Pocket Change</string>
    <string name="pocketchange_amount">Amount</string>
    
    <!-- Transition Names -->
    <string name="subaddress_info_transition_name" translatable="false">subaddress_info_transition</string>
    <string name="tx_details_transition_name" translatable="false">tx_details_transition</string>
    <string name="subaddress_txinfo_transition_name" translatable="false">subaddress_txinfo_transition</string>
    
    <!-- Node Settings -->
    <string name="node_use_ssl">Use SSL</string>
'

# Find the closing </resources> tag and insert before it
if grep -q "</resources>" "$STRINGS_FILE"; then
    # Use awk to insert the missing strings before </resources>
    awk -v strings="$MISSING_STRINGS" '
        /<\/resources>/ {
            print strings
        }
        { print }
    ' "$STRINGS_FILE" > "${STRINGS_FILE}.tmp"
    
    mv "${STRINGS_FILE}.tmp" "$STRINGS_FILE"
    echo "✓ Added missing string resources"
else
    echo "Error: Could not find </resources> tag in $STRINGS_FILE"
    exit 1
fi

# Show what was added
echo ""
echo "Added the following strings:"
echo "$MISSING_STRINGS"
echo ""
echo "To restore original file: mv ${STRINGS_FILE}.backup $STRINGS_FILE"

# Also need to fix the XML preferences file namespace issues
PREFS_FILE="app/src/main/res/xml/root_preferences.xml"

if [ -f "$PREFS_FILE" ]; then
    echo ""
    echo "Checking $PREFS_FILE for namespace issues..."
    
    # Create backup
    cp "$PREFS_FILE" "${PREFS_FILE}.backup"
    
    # Check if proper namespace is declared
    if ! grep -q 'xmlns:app="http://schemas.android.com/apk/res-auto"' "$PREFS_FILE"; then
        echo "⚠ Warning: $PREFS_FILE may be missing xmlns:app namespace"
        echo "The file should have xmlns:app=\"http://schemas.android.com/apk/res-auto\" in the root element"
        echo "Example: <PreferenceScreen xmlns:android=\"http://schemas.android.com/apk/res/android\""
        echo "                          xmlns:app=\"http://schemas.android.com/apk/res-auto\">"
    fi
fi

echo ""
echo "Build should now succeed. Run: ./gradlew clean assembleDebug"
