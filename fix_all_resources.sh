#!/bin/bash

# Complete fix for preferences and strings

set -e

echo "=== Fixing Android Resources ==="
echo ""

STRINGS_FILE="app/src/main/res/values/strings.xml"
PREFS_FILE="app/src/main/res/xml/root_preferences.xml"

# Backup files
[ -f "$STRINGS_FILE" ] && cp "$STRINGS_FILE" "${STRINGS_FILE}.backup"
[ -f "$PREFS_FILE" ] && cp "$PREFS_FILE" "${PREFS_FILE}.backup"

echo "=== Step 1: Adding missing string and array resources ==="

# Add missing resources before </resources>
MISSING_RESOURCES='
    <!-- Preference Keys (used as identifiers) -->
    <string name="preferred_locale" translatable="false">preferred_locale</string>
    <string name="preferred_nightmode" translatable="false">preferred_nightmode</string>
    <string name="preferred_theme" translatable="false">preferred_theme</string>
    <string name="preferred_lock" translatable="false">preferred_lock</string>
    <string name="preferred_stickyfiat" translatable="false">preferred_stickyfiat</string>
    <string name="credits_info" translatable="false">credits_info</string>
    <string name="privacy_info" translatable="false">privacy_info</string>
    <string name="about_info" translatable="false">about_info</string>

    <!-- Array Values for Preferences -->
    <string-array name="daynight_values" translatable="false">
        <item>AUTO</item>
        <item>DAY</item>
        <item>NIGHT</item>
    </string-array>

    <string-array name="themes_values" translatable="false">
        <item>Classic</item>
        <item>Oled</item>
        <item>BrightBlue</item>
    </string-array>
'

if [ -f "$STRINGS_FILE" ]; then
    awk -v resources="$MISSING_RESOURCES" '
        /<\/resources>/ && !done {
            print resources
            done=1
        }
        { print }
    ' "$STRINGS_FILE" > "${STRINGS_FILE}.tmp"
    
    mv "${STRINGS_FILE}.tmp" "$STRINGS_FILE"
    echo "✓ Added missing resources to strings.xml"
fi

echo ""
echo "=== Step 2: Fixing root_preferences.xml ==="

if [ -f "$PREFS_FILE" ]; then
    cat > "$PREFS_FILE" << 'EOF'
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android"
                  xmlns:app="http://schemas.android.com/apk/res-auto">
    <PreferenceCategory app:title="@string/title_iface">
        <ListPreference
            app:key="preferred_locale"
            app:defaultValue=""
            app:title="@string/menu_language"
            app:useSimpleSummaryProvider="true" />
        <ListPreference
            app:key="preferred_nightmode"
            app:defaultValue="AUTO"
            app:entries="@array/daynight_themes"
            app:entryValues="@array/daynight_values"
            app:title="@string/setting_daynight"
            app:useSimpleSummaryProvider="true" />
        <ListPreference
            app:key="preferred_theme"
            app:defaultValue="Classic"
            app:entries="@array/themes"
            app:entryValues="@array/themes_values"
            app:title="@string/setting_theme"
            app:useSimpleSummaryProvider="true" />
        <SwitchPreference
            app:key="preferred_lock"
            app:defaultValue="false"
            android:title="@string/setting_lock" />
        <SwitchPreference
            app:key="preferred_stickyfiat"
            app:defaultValue="false"
            android:title="@string/setting_stickyfiat"
            android:summary="@string/setting_stickyfiat_summary" />
    </PreferenceCategory>
    <PreferenceCategory app:title="@string/title_info">
        <Preference
            app:key="credits_info"
            app:title="@string/label_credits" />
        <Preference
            app:key="privacy_info"
            app:title="@string/menu_privacy" />
        <Preference
            app:key="about_info"
            app:title="@string/menu_about" />
    </PreferenceCategory>
</PreferenceScreen>
EOF
    echo "✓ Fixed root_preferences.xml"
fi

echo ""
echo "=== Summary of Changes ==="
echo "1. Added preference key strings (non-translatable)"
echo "2. Added missing array values (daynight_values, themes_values)"
echo "3. Fixed root_preferences.xml:"
echo "   - Changed app:key from @string/... to plain values"
echo "   - Fixed all attributes to use app: prefix correctly"
echo "   - Fixed SwitchPreference to use app:key and app:defaultValue"
echo ""
echo "=== Build the project ==="
echo "Run: ./gradlew clean assembleDebug"
echo ""
echo "=== Restore backups if needed ==="
echo "mv ${STRINGS_FILE}.backup $STRINGS_FILE"
echo "mv ${PREFS_FILE}.backup $PREFS_FILE"
