#!/bin/bash
# Auto-generated fix script for string resources
# Run this to apply common fixes

echo '=== FIXING STRING RESOURCES ==='

# Backup strings.xml
cp app/src/main/res/values/strings.xml app/src/main/res/values/strings.xml.backup.$(date +%Y%m%d_%H%M%S)

# Add missing resources to strings.xml
cat >> app/src/main/res/values/strings.xml << 'EOF'

    <!-- Added by fix script -->
    <string name="storage_permission_android_13">Requesting Android 13+ storage permissions</string>
    <string name="storage_permission_android_11">Requesting Android 11+ storage permissions</string>
    <string name="storage_permission_legacy">Requesting legacy storage permissions</string>
    <string name="storage_permission_manage_external_denied">MANAGE_EXTERNAL_STORAGE not granted - some features may be limited</string>
    <string name="storage_permission_manage_external_granted">MANAGE_EXTERNAL_STORAGE granted - full access available</string>
    <string name="permission_rationale_shown">Permission rationale shown</string>
    <string name="error_wallet_not_initialized">Wallet not initialized</string>
    <string name="error_invalid_address">Invalid address</string>
    <string name="error_network">Network error</string>
    <string name="success_transaction">Transaction sent successfully</string>
    <string name="success_operation">Operation completed successfully</string>

EOF

echo 'Added new string resources'

# Manual fixes needed:
echo ''
echo '=== MANUAL FIXES NEEDED ==='
echo '1. Check PermissionHandler.kt for Timber logs that should use resources:'
echo '   - Replace hardcoded strings with context.getString(R.string.XXX)'
echo ''
echo '2. Check UI files in /ui/ directory:'
echo '   - Replace strings in Text(), Toast(), Snackbar() with stringResource()'
echo ''
echo '3. For Compose files, use stringResource(R.string.xxx)'
echo '   For Android Views, use getString(R.string.xxx)'
echo '   For Timber logs, consider if they need to be user-facing'

echo '=== DONE ==='