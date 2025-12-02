#!/bin/bash

# Script to update themes.xml and strings.xml for Apo Monero Wallet
# Run from project root: ~/git/apo

set -e  # Exit on error

echo "=========================================="
echo "Apo Resource Update Script"
echo "=========================================="
echo ""

# Navigate to project root
cd ~/git/apo

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# ============================================
# 1. UPDATE themes.xml
# ============================================
echo -e "${YELLOW}[1/4] Updating themes.xml...${NC}"

THEMES_FILE="app/src/main/res/values/themes.xml"

# Backup existing file if it exists
if [ -f "$THEMES_FILE" ]; then
    cp "$THEMES_FILE" "$THEMES_FILE.backup"
    echo "  ✓ Backed up existing themes.xml"
fi

# Create themes.xml with Material3 theme
cat > "$THEMES_FILE" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    
    <!-- Base application theme - Material3 for Compose -->
    <style name="Theme.Apo" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Primary brand color - Monero Orange -->
        <item name="colorPrimary">#FF6600</item>
        <item name="colorOnPrimary">#FFFFFF</item>
        <item name="colorPrimaryContainer">#FF8833</item>
        <item name="colorOnPrimaryContainer">#000000</item>
        
        <!-- Secondary brand color -->
        <item name="colorSecondary">#4D4D4D</item>
        <item name="colorOnSecondary">#FFFFFF</item>
        <item name="colorSecondaryContainer">#6D6D6D</item>
        <item name="colorOnSecondaryContainer">#FFFFFF</item>
        
        <!-- Tertiary colors -->
        <item name="colorTertiary">#4CAF50</item>
        <item name="colorOnTertiary">#FFFFFF</item>
        
        <!-- Background colors -->
        <item name="android:colorBackground">#0F0F0F</item>
        <item name="colorSurface">#1A1A1A</item>
        <item name="colorOnBackground">#E0E0E0</item>
        <item name="colorOnSurface">#E0E0E0</item>
        
        <!-- Error colors -->
        <item name="colorError">#FF6600</item>
        <item name="colorOnError">#FFFFFF</item>
        
        <!-- Status bar and navigation bar -->
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar" tools:targetApi="m">false</item>
        <item name="android:windowLightNavigationBar" tools:targetApi="o_mr1">false</item>
        
        <!-- Window properties -->
        <item name="android:windowDrawsSystemBarBackgrounds">true</item>
        <item name="android:enforceNavigationBarContrast" tools:targetApi="q">false</item>
        
        <!-- Enable edge-to-edge -->
        <item name="android:windowLayoutInDisplayCutoutMode" tools:targetApi="p">shortEdges</item>
    </style>
    
    <!-- Splash screen theme (optional) -->
    <style name="Theme.Apo.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">#0F0F0F</item>
        <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>
        <item name="postSplashScreenTheme">@style/Theme.Apo</item>
    </style>
    
</resources>
EOF

echo -e "${GREEN}  ✓ themes.xml created successfully${NC}"

# ============================================
# 2. UPDATE strings.xml
# ============================================
echo -e "${YELLOW}[2/4] Updating strings.xml...${NC}"

STRINGS_FILE="app/src/main/res/values/strings.xml"

# Backup existing file if it exists
if [ -f "$STRINGS_FILE" ]; then
    cp "$STRINGS_FILE" "$STRINGS_FILE.backup"
    echo "  ✓ Backed up existing strings.xml"
fi

# Create strings.xml with all required strings
cat > "$STRINGS_FILE" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- App Name -->
    <string name="app_name">Apo</string>
    
    <!-- Main Navigation -->
    <string name="nav_wallet">Wallet</string>
    <string name="nav_send">Send</string>
    <string name="nav_receive">Receive</string>
    <string name="nav_history">History</string>
    <string name="nav_settings">Settings</string>
    
    <!-- Wallet Screen -->
    <string name="wallet_total_balance">Total Balance</string>
    <string name="wallet_unlocked_balance">Unlocked: %s XMR</string>
    <string name="wallet_syncing">Syncing: %1$.1f%% (%2$d / %3$d)</string>
    <string name="wallet_your_address">Your Address</string>
    <string name="wallet_height">Height</string>
    <string name="wallet_network">Network</string>
    
    <!-- Quick Actions -->
    <string name="action_receive">Receive</string>
    <string name="action_send">Send</string>
    <string name="action_exchange">Exchange</string>
    <string name="action_refresh">Refresh</string>
    
    <!-- Send Screen -->
    <string name="send_title">Send Monero</string>
    <string name="send_available_balance">Available Balance</string>
    <string name="send_recipient_address">Recipient Address</string>
    <string name="send_amount">Amount (XMR)</string>
    <string name="send_max">MAX</string>
    <string name="send_scan_qr">Scan QR</string>
    <string name="send_transaction">Send Transaction</string>
    <string name="send_confirm_title">Confirm Transaction</string>
    <string name="send_confirm_amount">Amount:</string>
    <string name="send_confirm_to">To:</string>
    <string name="send_confirm_warning">This transaction cannot be reversed. Please verify the details carefully.</string>
    <string name="send_confirm_button">Confirm &amp; Send</string>
    
    <!-- Receive Screen -->
    <string name="receive_title">Receive Monero</string>
    <string name="receive_your_address">Your Monero Address</string>
    <string name="receive_qr_code">QR Code</string>
    <string name="receive_amount_optional">Amount (optional)</string>
    <string name="receive_copy_address">Copy Address</string>
    <string name="receive_address_copied">Copied!</string>
    <string name="receive_share_info">Share this address to receive Monero payments</string>
    
    <!-- History Screen -->
    <string name="history_title">Transaction History</string>
    <string name="history_no_transactions">No transactions yet</string>
    <string name="history_received">Received</string>
    <string name="history_sent">Sent</string>
    <string name="history_confirmed">Confirmed</string>
    <string name="history_pending">Pending</string>
    
    <!-- Settings Screen -->
    <string name="settings_title">Settings</string>
    <string name="settings_wallet_info">Wallet Information</string>
    <string name="settings_status">Status</string>
    <string name="settings_syncing">Syncing</string>
    <string name="settings_ready">Ready</string>
    <string name="settings_not_ready">Not Ready</string>
    
    <!-- Maintenance -->
    <string name="settings_maintenance">Maintenance</string>
    <string name="settings_rescan_blockchain">Rescan Blockchain</string>
    <string name="settings_rescan_subtitle">Fix missing transactions</string>
    <string name="settings_rescan_progress">Scanning… %.1f%%</string>
    <string name="settings_force_refresh">Force Balance Refresh</string>
    <string name="settings_force_refresh_subtitle">Recalculate wallet balance</string>
    
    <!-- Backup & Security -->
    <string name="settings_backup_security">Backup &amp; Security</string>
    <string name="settings_view_seed">View Seed Phrase</string>
    <string name="settings_view_seed_subtitle">Backup your wallet recovery phrase</string>
    <string name="settings_export_keys">Export Keys</string>
    <string name="settings_export_keys_subtitle">Export private keys</string>
    
    <!-- Network -->
    <string name="settings_network">Network</string>
    <string name="settings_node_settings">Node Settings</string>
    <string name="settings_current_node">Current: %1$s:%2$d</string>
    <string name="settings_reload_config">Reload Configuration</string>
    <string name="settings_reload_config_subtitle">Refresh wallet configuration</string>
    
    <!-- Advanced -->
    <string name="settings_advanced">Advanced</string>
    <string name="settings_tx_search">Transaction Search</string>
    <string name="settings_tx_search_subtitle">Search for missing transaction by ID</string>
    <string name="settings_security">Security</string>
    <string name="settings_security_subtitle">Password &amp; privacy settings</string>
    
    <!-- Dialogs -->
    <string name="dialog_close">Close</string>
    <string name="dialog_cancel">Cancel</string>
    <string name="dialog_confirm">Confirm</string>
    <string name="dialog_yes">Yes</string>
    <string name="dialog_no">No</string>
    
    <!-- Rescan Dialog -->
    <string name="rescan_title">Rescan Blockchain</string>
    <string name="rescan_message">This will rescan the blockchain to find missing transactions. This may take several minutes. Continue?</string>
    <string name="rescan_start">Start Rescan</string>
    
    <!-- Seed Phrase Dialog -->
    <string name="seed_title">Seed Phrase</string>
    <string name="seed_warning">⚠️ Never share your seed phrase with anyone!</string>
    <string name="seed_info">Your seed phrase is the master key to your wallet. Store it securely offline.</string>
    <string name="seed_placeholder">[Seed phrase would be displayed here - implement wallet.getSeed() method]</string>
    
    <!-- Node Configuration Dialog -->
    <string name="node_title">Node Configuration</string>
    <string name="node_current_daemon">Current daemon:</string>
    <string name="node_change_info">To change the node, update wallet.properties file.</string>
    
    <!-- Transaction Search Dialog -->
    <string name="tx_search_title">Search Transaction</string>
    <string name="tx_search_info">Enter transaction ID to search for missing transactions</string>
    <string name="tx_search_id">Transaction ID</string>
    <string name="tx_search_button">Search</string>
    <string name="tx_search_found">Transaction found!\nAmount: %1$s XMR\nConfirmations: %2$d</string>
    <string name="tx_search_not_found">Transaction not found in wallet history</string>
    <string name="tx_search_error">Error: %s</string>
    
    <!-- Initialization -->
    <string name="init_wallet">Initializing wallet…</string>
    <string name="init_wait">This may take a moment…</string>
    
    <!-- Errors -->
    <string name="error_wallet_not_ready">Wallet not initialized</string>
    <string name="error_invalid_amount">Invalid amount</string>
    <string name="error_insufficient_balance">Insufficient balance. Required: %1$s XMR, Available: %2$s XMR</string>
    <string name="error_wallet_busy">Wallet busy: %s</string>
    <string name="error_transaction_failed">Transaction failed: %s</string>
    
    <!-- Success Messages -->
    <string name="success_transaction_sent">Transaction sent!\nTxID: %s</string>
    
    <!-- QR Code -->
    <string name="qr_add_library">Add QRCodeGenerator library</string>
    
</resources>
EOF

echo -e "${GREEN}  ✓ strings.xml created successfully${NC}"

# ============================================
# 3. VERIFY build.gradle.kts
# ============================================
echo -e "${YELLOW}[3/4] Verifying build.gradle.kts dependencies...${NC}"

BUILD_GRADLE="app/build.gradle.kts"

# Check for Material3
if grep -q "androidx.compose.material3:material3" "$BUILD_GRADLE"; then
    echo -e "${GREEN}  ✓ Material3 dependency found${NC}"
else
    echo -e "${RED}  ✗ Material3 dependency missing!${NC}"
    exit 1
fi

# Check for Material Icons Extended
if grep -q "androidx.compose.material:material-icons-extended" "$BUILD_GRADLE"; then
    echo -e "${GREEN}  ✓ Material Icons Extended dependency found${NC}"
else
    echo -e "${RED}  ✗ Material Icons Extended dependency missing!${NC}"
    exit 1
fi

# Check for Compose BOM
if grep -q "androidx.compose:compose-bom" "$BUILD_GRADLE"; then
    echo -e "${GREEN}  ✓ Compose BOM found${NC}"
else
    echo -e "${RED}  ✗ Compose BOM missing!${NC}"
    exit 1
fi

# Check for Activity Compose
if grep -q "androidx.activity:activity-compose" "$BUILD_GRADLE"; then
    echo -e "${GREEN}  ✓ Activity Compose dependency found${NC}"
else
    echo -e "${RED}  ✗ Activity Compose dependency missing!${NC}"
    exit 1
fi

echo -e "${GREEN}  ✓ All required dependencies present in build.gradle.kts${NC}"

# ============================================
# 4. CREATE NIGHT MODE themes.xml
# ============================================
echo -e "${YELLOW}[4/4] Creating night mode themes (values-night)...${NC}"

THEMES_NIGHT_DIR="app/src/main/res/values-night"
mkdir -p "$THEMES_NIGHT_DIR"

THEMES_NIGHT_FILE="$THEMES_NIGHT_DIR/themes.xml"

cat > "$THEMES_NIGHT_FILE" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    
    <!-- Night theme - inherits from base with darker colors -->
    <style name="Theme.Apo" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Primary brand color - Monero Orange -->
        <item name="colorPrimary">#FF6600</item>
        <item name="colorOnPrimary">#FFFFFF</item>
        <item name="colorPrimaryContainer">#CC5200</item>
        <item name="colorOnPrimaryContainer">#FFFFFF</item>
        
        <!-- Secondary brand color -->
        <item name="colorSecondary">#6D6D6D</item>
        <item name="colorOnSecondary">#FFFFFF</item>
        <item name="colorSecondaryContainer">#4D4D4D</item>
        <item name="colorOnSecondaryContainer">#E0E0E0</item>
        
        <!-- Tertiary colors -->
        <item name="colorTertiary">#4CAF50</item>
        <item name="colorOnTertiary">#FFFFFF</item>
        
        <!-- Background colors - Darker for night mode -->
        <item name="android:colorBackground">#000000</item>
        <item name="colorSurface">#0F0F0F</item>
        <item name="colorOnBackground">#E0E0E0</item>
        <item name="colorOnSurface">#E0E0E0</item>
        
        <!-- Error colors -->
        <item name="colorError">#FF6600</item>
        <item name="colorOnError">#FFFFFF</item>
        
        <!-- Status bar and navigation bar -->
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar" tools:targetApi="m">false</item>
        <item name="android:windowLightNavigationBar" tools:targetApi="o_mr1">false</item>
        
        <!-- Window properties -->
        <item name="android:windowDrawsSystemBarBackgrounds">true</item>
        <item name="android:enforceNavigationBarContrast" tools:targetApi="q">false</item>
        
        <!-- Enable edge-to-edge -->
        <item name="android:windowLayoutInDisplayCutoutMode" tools:targetApi="p">shortEdges</item>
    </style>
    
</resources>
EOF

echo -e "${GREEN}  ✓ Night mode themes.xml created${NC}"

# ============================================
# SUMMARY
# ============================================
echo ""
echo "=========================================="
echo -e "${GREEN}✓ Resource Update Complete!${NC}"
echo "=========================================="
echo ""
echo "Updated files:"
echo "  • $THEMES_FILE"
echo "  • $STRINGS_FILE"
echo "  • $THEMES_NIGHT_FILE"
echo ""
echo "Backups created (if files existed):"
echo "  • $THEMES_FILE.backup"
echo "  • $STRINGS_FILE.backup"
echo ""
echo -e "${GREEN}All dependencies verified in build.gradle.kts ✓${NC}"
echo ""
echo "Next steps:"
echo "  1. Sync Gradle: ./gradlew sync"
echo "  2. Clean build: ./gradlew clean"
echo "  3. Build APK: ./gradlew assembleDebug"
echo ""
echo "Your app is ready to build! 🚀"
