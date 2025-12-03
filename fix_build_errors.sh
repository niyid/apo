#!/bin/bash

# Script to fix Android build errors for Apo wallet
# This script addresses missing string resources and splash screen issues

set -e

PROJECT_DIR="$HOME/git/apo"
STRINGS_FILE="$PROJECT_DIR/app/src/main/res/values/strings.xml"
STRINGS_TA_FILE="$PROJECT_DIR/app/src/main/res/values-ta/strings.xml"
THEMES_FILE="$PROJECT_DIR/app/src/main/res/values/themes.xml"
BUILD_GRADLE="$PROJECT_DIR/app/build.gradle"

echo "=== Apo Wallet Build Fixer ==="
echo "Project directory: $PROJECT_DIR"
echo ""

# Backup existing files
echo "Creating backups..."
cp "$STRINGS_FILE" "$STRINGS_FILE.backup.$(date +%s)" 2>/dev/null || true
cp "$THEMES_FILE" "$THEMES_FILE.backup.$(date +%s)" 2>/dev/null || true

# Fix 1: Add missing string resources to values/strings.xml
echo "Adding missing string resources..."

cat >> "$STRINGS_FILE" << 'EOF'

    <!-- Additional Missing Resources -->
    <string name="action_copy">Copy</string>
    <string name="common_yes">Yes</string>
    <string name="common_no">No</string>
    <string name="status_copied">Copied!</string>
    
    <!-- Missing from original app -->
    <string name="accounts_drawer_new">New Account</string>
    <string name="accounts_new">Accounts</string>
    <string name="accounts_progress_new">Creating account...</string>
    <string name="archive_progress">Archiving...</string>
    <string name="backup_failed">Backup failed</string>
    <string name="backup_progress">Backing up...</string>
    <string name="backup_success">Backup successful</string>
    <string name="bad_fingerprint">Invalid fingerprint</string>
    <string name="bad_ledger_seed">Invalid Ledger seed</string>
    <string name="bad_password">Invalid password</string>
    <string name="bad_saved_password">Saved password is incorrect</string>
    <string name="bad_wallet">Invalid wallet</string>
    <string name="bluetooth_permissions">Bluetooth permissions required</string>
    <string name="bluetooth_permissions_cancel">Cancel</string>
    <string name="bluetooth_permissions_ok">OK</string>
    <string name="bluetooth_permissions_settings">Settings</string>
    <string name="bluetooth_select_label">Select Bluetooth device</string>
    <string name="changepw_failed">Password change failed</string>
    <string name="changepw_progress">Changing password...</string>
    <string name="changepw_success">Password changed successfully</string>
    <string name="connect_stagenet">Connect to stagenet</string>
    <string name="connect_testnet">Connect to testnet</string>
    <string name="copy_receive_address">Copy receive address</string>
    <string name="delete_alert_message">Are you sure you want to delete this wallet?</string>
    <string name="delete_alert_no">No</string>
    <string name="delete_alert_yes">Yes</string>
    <string name="delete_failed">Delete failed</string>
    <string name="deletecache_alert_message">Delete cached blockchain data?</string>
    <string name="details_alert_message">Show wallet details?</string>
    <string name="details_alert_no">No</string>
    <string name="details_alert_yes">Yes</string>
    <string name="details_title">Wallet Details</string>
    <string name="fab_create_new">Create New Wallet</string>
    <string name="fab_restore_key">Restore from Keys</string>
    <string name="fab_restore_ledger">Restore from Ledger</string>
    <string name="fab_restore_seed">Restore from Seed</string>
    <string name="fab_restore_sidekick">Restore from Sidekick</string>
    <string name="fab_restore_viewonly">Restore View-Only</string>
    <string name="generate_address_hint">Address</string>
    <string name="generate_address_label">Public Address</string>
    <string name="generate_address_label_sub">Subaddress</string>
    <string name="generate_bad_passwordB">Passwords don\'t match</string>
    <string name="generate_buttonGenerate">Generate Wallet</string>
    <string name="generate_button_accept">Accept</string>
    <string name="generate_check_address">Address is required</string>
    <string name="generate_check_key">Keys are required</string>
    <string name="generate_check_mnemonic">Seed phrase required</string>
    <string name="generate_crazypass_label">Password strength</string>
    <string name="generate_empty_passwordB">Password cannot be empty</string>
    <string name="generate_fingerprint_hint">Use fingerprint</string>
    <string name="generate_fingerprint_warn">Fingerprint will be used to unlock</string>
    <string name="generate_mnemonic_hint">Seed Phrase (25 words)</string>
    <string name="generate_mnemonic_label">Mnemonic Seed</string>
    <string name="generate_name_hint">Wallet Name</string>
    <string name="generate_password_hint">Password</string>
    <string name="generate_restoreheight_error">Invalid restore height</string>
    <string name="generate_restoreheight_hint">Restore Height (optional)</string>
    <string name="generate_spendkey_hint">Spend Key</string>
    <string name="generate_spendkey_label">Spend Key</string>
    <string name="generate_title">Create Wallet</string>
    <string name="generate_viewkey_hint">View Key</string>
    <string name="generate_viewkey_label">View Key</string>
    <string name="generate_wallet_created">Wallet created</string>
    <string name="generate_wallet_creating">Creating wallet...</string>
    <string name="generate_wallet_dot">.</string>
    <string name="generate_wallet_exists">Wallet already exists</string>
    <string name="generate_wallet_name">Wallet Name</string>
    <string name="generate_wallet_type_key">Restore from Keys</string>
    <string name="generate_wallet_type_ledger">Restore from Ledger</string>
    <string name="generate_wallet_type_new">Create New</string>
    <string name="generate_wallet_type_seed">Restore from Seed</string>
    <string name="generate_wallet_type_view">View Only</string>
    <string name="gunther_says">Gunther says</string>
    <string name="info_ledger_enabled">Ledger enabled</string>
    <string name="info_nodes_enabled">Nodes enabled</string>
    <string name="info_paymentid_integrated">Payment ID integrated</string>
    <string name="info_prepare_tx">Preparing transaction...</string>
    <string name="info_send_xmrto_error">XMR.to error</string>
    <string name="info_send_xmrto_paid">Payment received</string>
    <string name="info_send_xmrto_parms">Parameters</string>
    <string name="info_send_xmrto_query">Querying...</string>
    <string name="info_send_xmrto_sent">Sent</string>
    <string name="info_send_xmrto_success_btc">BTC sent successfully</string>
    <string name="info_send_xmrto_success_order_label">Order ID</string>
    <string name="info_send_xmrto_unpaid">Unpaid</string>
    <string name="info_xmrto">XMR.to</string>
    <string name="info_xmrto_ambiguous">Ambiguous amount</string>
    <string name="info_xmrto_enabled">XMR.to enabled</string>
    <string name="info_xmrto_help">Help</string>
    <string name="info_xmrto_help_xmr">XMR Amount</string>
    <string name="label_apply">Apply</string>
    <string name="label_bluetooth">Bluetooth</string>
    <string name="label_cancel">Cancel</string>
    <string name="label_close">Close</string>
    <string name="label_copy_address">Copy Address</string>
    <string name="label_copy_viewkey">Copy View Key</string>
    <string name="label_copy_xmrtokey">Copy XMR.to Key</string>
    <string name="label_credits">Credits</string>
    <string name="label_daemon">Daemon</string>
    <string name="label_generic_xmrto_error">XMR.to Error</string>
    <string name="label_login_wallets">Wallets</string>
    <string name="label_nodes">Nodes</string>
    <string name="label_ok">OK</string>
    <string name="label_receive_info_gen_qr_code">Generate QR Code</string>
    <string name="label_restoreheight">Restore Height</string>
    <string name="label_seed_offset_encrypt">Seed Offset (optional)</string>
    <string name="label_send_address">Address</string>
    <string name="label_send_btc_address">BTC Address</string>
    <string name="label_send_btc_amount">BTC Amount</string>
    <string name="label_send_btc_xmrto_info">XMR.to Info</string>
    <string name="label_send_btc_xmrto_key">XMR.to Key</string>
    <string name="label_send_btc_xmrto_key_lb">Key</string>
    <string name="label_send_done">Done</string>
    <string name="label_send_notes">Notes</string>
    <string name="label_send_progress_create_tx">Creating transaction...</string>
    <string name="label_send_progress_queryparms">Querying parameters...</string>
    <string name="label_send_progress_xmrto_create">Creating XMR.to order...</string>
    <string name="label_send_progress_xmrto_query">Querying XMR.to...</string>
    <string name="label_send_success">Success!</string>
    <string name="label_send_txid">Transaction ID</string>
    <string name="label_streetmode">Street Mode</string>
    <string name="label_test">Test</string>
    <string name="label_wallet_advanced_details">Advanced Details</string>
    <string name="label_wallet_receive">Receive</string>
    <string name="label_wallet_send">Send</string>
    <string name="label_watchonly">Watch Only</string>
    <string name="language_system_default">System Default</string>
    <string name="max_account_warning">Maximum accounts reached</string>
    <string name="max_subaddress_warning">Maximum subaddresses reached</string>
    <string name="menu_about">About</string>
    <string name="menu_back">Back</string>
    <string name="menu_backup">Backup</string>
    <string name="menu_bluetooth">Bluetooth</string>
    <string name="menu_cancel">Cancel</string>
    <string name="menu_changepw">Change Password</string>
    <string name="menu_close">Close</string>
    <string name="menu_daynight">Day/Night Mode</string>
    <string name="menu_default_nodes">Default Nodes</string>
    <string name="menu_delete">Delete</string>
    <string name="menu_deletecache">Delete Cache</string>
    <string name="menu_help">Help</string>
    <string name="menu_info">Info</string>
    <string name="menu_language">Language</string>
    <string name="menu_ledger_seed">Ledger Seed</string>
    <string name="menu_privacy">Privacy</string>
    <string name="menu_receive">Receive</string>
    <string name="menu_rename">Rename</string>
    <string name="menu_rescan">Rescan</string>
    <string name="menu_restore">Restore</string>
    <string name="menu_settings">Settings</string>
    <string name="menu_share">Share</string>
    <string name="menu_streetmode">Street Mode</string>
    <string name="message_camera_not_permitted">Camera permission denied</string>
    <string name="message_copy_address">Address copied</string>
    <string name="message_copy_txid">Transaction ID copied</string>
    <string name="message_copy_viewkey">View key copied</string>
    <string name="message_copy_xmrtokey">XMR.to key copied</string>
    <string name="message_exchange_failed">Exchange failed</string>
    <string name="message_nocopy">Cannot copy</string>
    <string name="message_qr_failed">QR code generation failed</string>
    <string name="node_address_hint">Node Address</string>
    <string name="node_auth_error">Authentication error</string>
    <string name="node_create_hint">Add Node</string>
    <string name="node_fab_add">Add Node</string>
    <string name="node_general_error">Connection error</string>
    <string name="node_height">Height</string>
    <string name="node_host_empty">Host required</string>
    <string name="node_host_unresolved">Cannot resolve host</string>
    <string name="node_name_hint">Node Name</string>
    <string name="node_nobookmark">No bookmarked nodes</string>
    <string name="node_pass_hint">Password</string>
    <string name="node_port_hint">Port</string>
    <string name="node_port_numeric">Port must be numeric</string>
    <string name="node_port_range">Port must be 1-65535</string>
    <string name="node_pull_hint">Pull to refresh</string>
    <string name="node_refresh_hint">Refresh</string>
    <string name="node_refresh_wait">Please wait...</string>
    <string name="node_result">Result</string>
    <string name="node_result_label">Test Result</string>
    <string name="node_scanning">Scanning...</string>
    <string name="node_test_error">Test failed</string>
    <string name="node_testing">Testing...</string>
    <string name="node_tor_error">Tor connection error</string>
    <string name="node_updated_days">%d days ago</string>
    <string name="node_updated_hours">%d hours ago</string>
    <string name="node_updated_mins">%d minutes ago</string>
    <string name="node_updated_now">Just now</string>
    <string name="node_user_hint">Username</string>
    <string name="node_waiting">Waiting...</string>
    <string name="node_wrong_net">Wrong network</string>
    <string name="onboarding_agree">I Agree</string>
    <string name="onboarding_button_next">Next</string>
    <string name="onboarding_button_ready">Ready</string>
    <string name="onboarding_fpsend_information">Use fingerprint to send</string>
    <string name="onboarding_fpsend_title">Fingerprint Send</string>
    <string name="onboarding_nodes_information">Select your node</string>
    <string name="onboarding_nodes_title">Nodes</string>
    <string name="onboarding_seed_information">Backup your seed phrase</string>
    <string name="onboarding_seed_title">Seed Backup</string>
    <string name="onboarding_welcome_information">Welcome to Apo Wallet</string>
    <string name="onboarding_welcome_title">Welcome</string>
    <string name="onboarding_xmrto_information">Exchange XMR to BTC</string>
    <string name="onboarding_xmrto_title">XMR.to</string>
    <string name="open_wallet_ledger_missing">Ledger device not found</string>
    <string name="open_wallet_sidekick_missing">Sidekick not connected</string>
    <string name="password_fair">Fair</string>
    <string name="password_good">Good</string>
    <string name="password_strong">Strong</string>
    <string name="password_very_strong">Very Strong</string>
    <string name="password_weak">Weak</string>
    <string name="paste_give_address">Paste address</string>
    <string name="pocketchange_create_title">Create Pocket Change</string>
    <string name="pocketchange_info">Pocket change for small transactions</string>
    <string name="progress_ledger_confirm">Confirm on Ledger</string>
    <string name="progress_ledger_lookahead">Generating subaddresses...</string>
    <string name="progress_ledger_mlsag">Signing transaction...</string>
    <string name="progress_ledger_opentx">Opening transaction...</string>
    <string name="progress_ledger_progress">Progress: %d%%</string>
    <string name="progress_ledger_verify">Verifying...</string>
    <string name="prompt_changepw">New Password</string>
    <string name="prompt_changepwB">Confirm Password</string>
    <string name="prompt_daemon_missing">Daemon not configured</string>
    <string name="prompt_fingerprint_auth">Authenticate with fingerprint</string>
    <string name="prompt_ledger_phrase">Ledger Seed Phrase</string>
    <string name="prompt_ledger_seed">Enter Ledger seed</string>
    <string name="prompt_ledger_seed_warn">Warning: This will expose your Ledger seed</string>
    <string name="prompt_open_wallet">Open Wallet</string>
    <string name="prompt_password">Password</string>
    <string name="prompt_rename">New Name</string>
    <string name="prompt_wrong_net">Wrong network type</string>
    <string name="receive_amount_hint">Amount</string>
    <string name="receive_amount_nan">Not a valid number</string>
    <string name="receive_amount_negative">Amount must be positive</string>
    <string name="receive_amount_too_big">Amount too large</string>
    <string name="receive_cannot_open">Cannot open wallet</string>
    <string name="receive_desc_hint">Description (optional)</string>
    <string name="rename_failed">Rename failed</string>
    <string name="rename_progress">Renaming...</string>
    <string name="restore_failed">Restore failed</string>
    <string name="seed_offset_hint">Seed offset (advanced)</string>
    <string name="send_address_hint">Recipient address</string>
    <string name="send_address_invalid">Invalid address</string>
    <string name="send_address_no_dnssec">DNSSEC not available</string>
    <string name="send_address_not_openalias">Not an OpenAlias address</string>
    <string name="send_address_openalias">OpenAlias address</string>
    <string name="send_address_resolve_openalias">Resolving OpenAlias...</string>
    <string name="send_address_title">Address</string>
    <string name="send_amount_label">Amount</string>
    <string name="send_amount_title">Amount</string>
    <string name="send_available">Available</string>
    <string name="send_available_btc">BTC Available</string>
    <string name="send_create_tx_error_title">Create Transaction Error</string>
    <string name="send_fee">Fee</string>
    <string name="send_fee_btc_label">BTC Fee</string>
    <string name="send_fee_label">Network Fee</string>
    <string name="send_generate_paymentid_hint">Generate Payment ID</string>
    <string name="send_notes_hint">Notes (optional)</string>
    <string name="send_qr_address_invalid">Not a valid QR code</string>
    <string name="send_qr_hint">Scan QR Code</string>
    <string name="send_qr_invalid">Invalid QR code</string>
    <string name="send_send_label">Send</string>
    <string name="send_send_timed_label">Send (timed)</string>
    <string name="send_success_title">Success</string>
    <string name="send_sweepall">Send All</string>
    <string name="send_total_btc_label">Total BTC</string>
    <string name="send_total_label">Total</string>
    <string name="send_xmrto_timeout">XMR.to timeout</string>
    <string name="service_busy">Service busy</string>
    <string name="service_description">Apo Wallet Service</string>
    <string name="service_progress">In progress...</string>
    <string name="setting_daynight">Day/Night Mode</string>
    <string name="setting_lock">Lock Wallet</string>
    <string name="setting_stickyfiat">Sticky Fiat</string>
    <string name="setting_stickyfiat_summary">Remember fiat currency preference</string>
    <string name="setting_theme">Theme</string>
    <string name="shift_checkamount">Check amount</string>
    <string name="shift_noquote">No quote available</string>
    <string name="sidekick_confirm_tx">Confirm transaction on sidekick</string>
    <string name="sidekick_connected">Sidekick connected</string>
    <string name="sidekick_network_warning">Network warning</string>
    <string name="sidekick_not_connected">Sidekick not connected</string>
    <string name="sidekick_pin">Sidekick PIN</string>
    <string name="status_remaining">Remaining</string>
    <string name="status_synced">Synced</string>
    <string name="status_syncing">Syncing...</string>
    <string name="status_transaction_failed">Transaction failed</string>
    <string name="status_wallet_connect_failed">Connection failed</string>
    <string name="status_wallet_connect_ioex">I/O error</string>
    <string name="status_wallet_connect_wrongversion">Wrong version</string>
    <string name="status_wallet_connecting">Connecting...</string>
    <string name="status_wallet_disconnected">Disconnected</string>
    <string name="status_wallet_loading">Loading...</string>
    <string name="status_wallet_node_invalid">Invalid node</string>
    <string name="status_wallet_unload_failed">Unload failed</string>
    <string name="status_wallet_unloaded">Unloaded</string>
    <string name="street_sweep_amount">Sweep amount</string>
    <string name="subaddress_add">Add Subaddress</string>
    <string name="subaddress_details_hint">Details</string>
    <string name="subaddress_notx_label">No transactions</string>
    <string name="subaddress_select_label">Select subaddress</string>
    <string name="subaddress_tx_label">%d transactions</string>
    <string name="subbaddress_name_hint">Subaddress name</string>
    <string name="subbaddress_title">Subaddresses</string>
    <string name="text_generic_xmrto_error">XMR.to Error</string>
    <string name="text_noretry">Cannot retry</string>
    <string name="text_noretry_monero">Cannot retry Monero transaction</string>
    <string name="text_retry">Retry</string>
    <string name="text_send_btc_amount">Send %s BTC</string>
    <string name="text_send_btc_rate">Rate: %s</string>
    <string name="title_iface">Interface</string>
    <string name="title_info">Info</string>
    <string name="toast_default_nodes">Using default nodes</string>
    <string name="toast_ledger_attached">Ledger attached</string>
    <string name="toast_ledger_detached">Ledger detached</string>
    <string name="toast_ledger_start_app">Start Monero app on Ledger</string>
    <string name="tor_enable">Enable Tor</string>
    <string name="tor_enable_background">Tor background mode</string>
    <string name="tor_noshift">Tor not available for XMR.to</string>
    <string name="tx_account">Account</string>
    <string name="tx_amount">Amount</string>
    <string name="tx_blockheight">Block Height</string>
    <string name="tx_destination">Destination</string>
    <string name="tx_failed">Failed</string>
    <string name="tx_fee">Fee</string>
    <string name="tx_id">Transaction ID</string>
    <string name="tx_key">Transaction Key</string>
    <string name="tx_list_amount_failed">Failed</string>
    <string name="tx_list_amount_negative">Sent</string>
    <string name="tx_list_amount_positive">Received</string>
    <string name="tx_list_failed_text">Failed transaction</string>
    <string name="tx_list_fee">Fee: %s</string>
    <string name="tx_locked" formatted="false">Locked until block %1$d (in ~%2$d minutes)</string>
    <string name="tx_notes">Notes</string>
    <string name="tx_notes_hint">Transaction notes</string>
    <string name="tx_paymentId">Payment ID</string>
    <string name="tx_pending">Pending</string>
    <string name="tx_subaddress">Subaddress</string>
    <string name="tx_timestamp">Timestamp</string>
    <string name="tx_title">Transaction</string>
    <string name="tx_transfers">Transfers</string>
    <string name="wallet_activity_name">Wallet</string>
    <string name="xmr_unconfirmed_amount">+ %s XMR unconfirmed</string>

    <!-- Arrays -->
    <string-array name="daynight_themes">
        <item>Auto</item>
        <item>Day</item>
        <item>Night</item>
    </string-array>

    <string-array name="themes">
        <item>Classic</item>
        <item>Oled</item>
        <item>BrightBlue</item>
    </string-array>

    <!-- Splash Screen Resources -->
    <string name="settings_rescan_blockchain">Rescan Blockchain</string>

</resources>
EOF

echo "✓ Missing string resources added"

# Fix 2: Fix Tamil strings.xml formatting error
echo "Fixing Tamil strings.xml formatting..."
if [ -f "$STRINGS_TA_FILE" ]; then
    sed -i 's/<string name="tx_locked">/<string name="tx_locked" formatted="false">/' "$STRINGS_TA_FILE"
    echo "✓ Tamil strings.xml fixed"
else
    echo "⚠ Tamil strings.xml not found, skipping"
fi

# Fix 3: Add/update themes.xml with splash screen support
echo "Updating themes.xml..."

cat > "$THEMES_FILE" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Base application theme -->
    <style name="AppTheme" parent="Theme.Material3.DynamicColors.DayNight">
        <item name="colorPrimary">@color/orange_primary</item>
        <item name="colorPrimaryVariant">@color/orange_dark</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/gray_secondary</item>
        <item name="colorSecondaryVariant">@color/gray_dark</item>
        <item name="colorOnSecondary">@color/white</item>
        <item name="android:statusBarColor">@color/black</item>
        <item name="android:navigationBarColor">@color/black</item>
    </style>

    <!-- Splash Screen Theme (uses basic Theme.AppCompat to avoid splash screen library) -->
    <style name="Theme.SplashScreen" parent="Theme.AppCompat.DayNight.NoActionBar">
        <item name="android:windowBackground">@color/orange_primary</item>
        <item name="android:statusBarColor">@color/orange_primary</item>
        <item name="android:navigationBarColor">@color/orange_primary</item>
        <item name="android:windowDrawsSystemBarBackgrounds">true</item>
    </style>

    <color name="orange_primary">#FF6600</color>
    <color name="orange_dark">#FF4400</color>
    <color name="gray_secondary">#4D4D4D</color>
    <color name="gray_dark">#333333</color>
    <color name="white">#FFFFFF</color>
    <color name="black">#0F0F0F</color>
</resources>
EOF

echo "✓ themes.xml updated with splash screen theme"

# Fix 4: Remove splash screen library dependency if present
echo "Checking build.gradle for splash screen library..."
if grep -q "androidx.core:core-splashscreen" "$BUILD_GRADLE"; then
    echo "Removing splash screen library dependency..."
    sed -i '/androidx.core:core-splashscreen/d' "$BUILD_GRADLE"
    echo "✓ Splash screen library removed"
else
    echo "✓ No splash screen library found"
fi

# Fix 5: Clean and rebuild
echo ""
echo "Cleaning project..."
cd "$PROJECT_DIR"
./gradlew clean

echo ""
echo "=== Fix Summary ==="
echo "✓ Added missing string resources to values/strings.xml"
echo "✓ Fixed Tamil strings.xml formatting (tx_locked)"
echo "✓ Updated themes.xml with compatible splash screen theme"
echo "✓ Removed incompatible splash screen library"
echo "✓ Cleaned project"
echo ""
echo "Now run: ../server_extras/gradleBuildCmd.sh"
echo ""
echo "Backup files created with .backup.TIMESTAMP extension"
echo "If issues persist, restore backups and report to developers."
