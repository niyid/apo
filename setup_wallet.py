#!/usr/bin/env python3
"""
Setup script for Monero Wallet Android App
Implements:
- Task 4: Storage permissions handling
- Task 5: Default wallet.properties configuration
- Task 6: ProGuard rules for Monero JNI
"""

import os
import sys
from pathlib import Path

# Color codes for terminal output
class Colors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'

def print_header(message):
    print(f"\n{Colors.HEADER}{Colors.BOLD}{'='*60}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{message}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{'='*60}{Colors.ENDC}\n")

def print_success(message):
    print(f"{Colors.OKGREEN}✓ {message}{Colors.ENDC}")

def print_error(message):
    print(f"{Colors.FAIL}✗ {message}{Colors.ENDC}")

def print_warning(message):
    print(f"{Colors.WARNING}⚠ {message}{Colors.ENDC}")

def print_info(message):
    print(f"{Colors.OKCYAN}ℹ {message}{Colors.ENDC}")

# Task 5: Create default wallet.properties
def create_wallet_properties(assets_dir):
    """Create wallet.properties file in assets directory"""
    print_header("TASK 5: Creating wallet.properties")
    
    wallet_properties_content = """# Monero Wallet Configuration
# This file contains default settings for the Monero wallet

# ============================================================================
# DAEMON CONFIGURATION
# ============================================================================
# Using reliable stagenet node (matches WalletManager.java defaults)
daemon.address=stagenet.xmr-tw.org
daemon.port=38081
daemon.username=
daemon.password=
daemon.ssl=false

# ============================================================================
# WALLET CONFIGURATION
# ============================================================================
# Wallet name matches WalletManager.java default
wallet.name=bitchat_wallet_stagenet
wallet.password=bitchat_secure_pass
wallet.language=English
wallet.restore_height=0

# ============================================================================
# NETWORK CONFIGURATION
# ============================================================================
# Network type: 0=mainnet, 1=testnet, 2=stagenet
# Default: 2 (stagenet) - matches WalletManager.java
network.type=2

# ============================================================================
# SYNC CONFIGURATION
# ============================================================================
# Sync interval: how often to check for new blocks (milliseconds)
# Default: 10 minutes (600000ms)
sync.interval=600000

# Sync timeout: maximum time to wait for sync operation (milliseconds)
# Default: 2 hours (7200000ms) for full rescan
sync.timeout=7200000

# Periodic sync interval between automatic sync attempts
periodic.sync.interval=600000

# Rescan progress check interval
rescan.progress.check.interval=30000

# Rescan cooldown period
rescan.cooldown=600000

# ============================================================================
# PERFORMANCE SETTINGS
# ============================================================================
# Adjust these based on device capabilities
cache.size=100
connection.timeout=30000
read.timeout=60000

# UI update throttle (milliseconds) - prevents frame drops
ui.update.throttle=500

# ============================================================================
# TRANSACTION SETTINGS
# ============================================================================
# Ring size for transactions (minimum 11 as of protocol v15)
# Default: 15 (matches WalletSuite.java)
tx.mixin=15

# Transaction priority: default, low, medium, high
tx.priority=default

# ============================================================================
# SECURITY SETTINGS
# ============================================================================
# Enable biometric authentication (requires implementation in PermissionHandler)
security.biometric=false

# Enable PIN protection (requires implementation)
security.pin=false

# Auto-lock timeout (milliseconds) - 0 = disabled
security.autolock.timeout=0

# ============================================================================
# EXCHANGE INTEGRATION (ChangeNOW)
# ============================================================================
# Add your API key in gradle.properties: changenow.api.key=YOUR_KEY
exchange.enabled=false
exchange.provider=changenow
exchange.timeout=15000

# ============================================================================
# BACKUP SETTINGS
# ============================================================================
# Automatic backup configuration
backup.auto=true
backup.interval=86400000
backup.location=/Android/data/com.bitchat.droid/files

# Backup to SD card when available
backup.use.sdcard=true
backup.sdcard.path=/Android/data/com.bitchat.droid/files

# ============================================================================
# UI SETTINGS
# ============================================================================
ui.theme=dark
ui.language=en

# ============================================================================
# LOGGING
# ============================================================================
log.level=INFO
log.file.enabled=false
log.categories=

# ============================================================================
# ADVANCED SETTINGS
# ============================================================================
# Daemon connection timeout
daemon.connection.timeout=10000

# Maximum allowed amount (atomic units)
# Default: Long.MAX_VALUE (no limit)
max.allowed.amount=9223372036854775807

# Light wallet mode
light.wallet=false

# Trust daemon
trust.daemon=false

# Use SSL proxy
use.proxy=false
proxy.address=

# ============================================================================
# NOTES
# ============================================================================
# - stagenet.xmr-tw.org is a reliable stagenet node
# - For mainnet, use: node.moneroworld.com:18089 or node.xmr.to:18081
# - For testnet, use: testnet.xmr-tw.org:28081
# - Restore height: set to wallet creation block height for faster sync
# - Connection timeouts in milliseconds
# - Mixin (ring size) must be >= 11 for current Monero protocol
# - Network type must match WalletManager.java configuration
"""
    
    properties_path = assets_dir / "wallet.properties"
    
    try:
        # Create assets directory if it doesn't exist
        assets_dir.mkdir(parents=True, exist_ok=True)
        
        # Write the properties file
        with open(properties_path, 'w') as f:
            f.write(wallet_properties_content)
        
        print_success(f"Created {properties_path}")
        print_info("Configuration includes:")
        print_info("  - Reliable stagenet node: stagenet.xmr-tw.org:38081")
        print_info("  - Network type: stagenet (matches WalletManager.java)")
        print_info("  - Wallet name: bitchat_wallet_stagenet")
        print_info("  - Transaction mixin: 15")
        print_info("  - Sync interval: 10 minutes")
        print_info("  - UI update throttle: 500ms (prevents frame drops)")
        print_info("  - Connection timeouts configured for slow networks")
        
        return True
        
    except Exception as e:
        print_error(f"Failed to create wallet.properties: {e}")
        return False

# Task 4: Add storage permissions handling to MainActivity
def create_permission_handler(java_dir):
    """Create PermissionHandler.kt for storage permissions"""
    print_header("TASK 4: Creating Permission Handler")
    
    permission_handler_content = """package com.techducat.apo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import java.io.File

/**
 * Handles storage permissions for Android 6.0+ with special handling for Android 11+
 * Addresses SELinux permission denials seen in logs
 */
object PermissionHandler {
    private const val TAG = "PermissionHandler"
    private const val REQUEST_CODE_STORAGE = 1001
    private const val REQUEST_CODE_MANAGE_STORAGE = 1002

    // Required permissions based on Android version
    private val STORAGE_PERMISSIONS_PRE_33 = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val STORAGE_PERMISSIONS_33_PLUS = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES
    )

    /**
     * Check if storage permissions are granted
     */
    fun hasStoragePermissions(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                STORAGE_PERMISSIONS_33_PLUS.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ (API 30+)
                // For scoped storage, we primarily need READ_EXTERNAL_STORAGE
                ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                // Android 6-10
                STORAGE_PERMISSIONS_PRE_33.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
    }

    /**
     * Check if app has MANAGE_EXTERNAL_STORAGE permission (Android 11+)
     * This is needed for accessing /Android/data/ directories
     */
    fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Not required on older versions
        }
    }

    /**
     * Request storage permissions
     */
    fun requestStoragePermissions(activity: Activity) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+
                Log.i(TAG, "Requesting Android 13+ storage permissions")
                ActivityCompat.requestPermissions(
                    activity,
                    STORAGE_PERMISSIONS_33_PLUS,
                    REQUEST_CODE_STORAGE
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+
                Log.i(TAG, "Requesting Android 11+ storage permissions")
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE
                )
                
                // Optionally request MANAGE_EXTERNAL_STORAGE for /Android/data/ access
                if (!hasManageStoragePermission()) {
                    Log.w(TAG, "MANAGE_EXTERNAL_STORAGE not granted - some features may be limited")
                    // Uncomment below to request this permission (requires special justification in Play Store)
                    // requestManageStoragePermission(activity)
                }
            }
            else -> {
                // Android 6-10
                Log.i(TAG, "Requesting legacy storage permissions")
                ActivityCompat.requestPermissions(
                    activity,
                    STORAGE_PERMISSIONS_PRE_33,
                    REQUEST_CODE_STORAGE
                )
            }
        }
    }

    /**
     * Request MANAGE_EXTERNAL_STORAGE permission (Android 11+)
     * NOTE: This requires special declaration in Play Store
     */
    fun requestManageStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + activity.packageName)
                activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
                Log.i(TAG, "Launched MANAGE_EXTERNAL_STORAGE permission screen")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request MANAGE_EXTERNAL_STORAGE", e)
                // Fallback to general settings
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivity(intent)
            }
        }
    }

    /**
     * Check if permission should show rationale
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Get appropriate storage directory based on Android version
     * Addresses SELinux issues by using proper app-specific directories
     * 
     * CRITICAL: Uses app-specific external storage to avoid SELinux denials
     * seen in logs for shell_data_file context
     */
    fun getWalletStorageDir(context: Context): String {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+: MUST use app-specific external storage
                // This avoids SELinux denials: avc: denied { write } ... tcontext=u:object_r:shell_data_file:s0
                val externalDir = context.getExternalFilesDir(null)
                if (externalDir != null) {
                    Log.i(TAG, "Using app-specific external storage (Android 11+): ${externalDir.absolutePath}")
                    externalDir.absolutePath
                } else {
                    Log.w(TAG, "External storage unavailable, falling back to internal")
                    context.filesDir.absolutePath
                }
            }
            else -> {
                // Android 6-10: Can use traditional external storage
                val externalDir = context.getExternalFilesDir(null)
                if (externalDir != null) {
                    Log.i(TAG, "Using external storage: ${externalDir.absolutePath}")
                    externalDir.absolutePath
                } else {
                    Log.w(TAG, "External storage unavailable, using internal")
                    context.filesDir.absolutePath
                }
            }
        }
    }

    /**
     * Get backup directory (separate from main wallet storage)
     * For SD card backup restoration as seen in WalletSuite.java
     */
    fun getBackupStorageDir(context: Context): String {
        // Try to use the same path as WalletSuite.java line 1002
        // File sdcardDir = new File(Environment.getExternalStorageDirectory(), "Android/data/com.bitchat.droid/files")
        val backupDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: Use app-specific directory
            context.getExternalFilesDir("backups")
        } else {
            // Android 6-10: Can access shared storage
            val sharedBackup = File(Environment.getExternalStorageDirectory(), "Android/data/com.bitchat.droid/files")
            if (sharedBackup.exists() || sharedBackup.mkdirs()) {
                sharedBackup
            } else {
                context.getExternalFilesDir("backups")
            }
        }
        
        val path = backupDir?.absolutePath ?: "${context.filesDir.absolutePath}/backups"
        Log.i(TAG, "Backup storage directory: $path")
        return path
    }

    /**
     * Handle permission request result
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        when (requestCode) {
            REQUEST_CODE_STORAGE -> {
                if (grantResults.isNotEmpty() && 
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.i(TAG, "✓ Storage permissions granted")
                    onGranted()
                } else {
                    Log.w(TAG, "✗ Storage permissions denied")
                    onDenied()
                }
            }
        }
    }

    /**
     * Log current permission status (for debugging SELinux issues)
     */
    fun logPermissionStatus(context: Context) {
        Log.d(TAG, "=== PERMISSION STATUS ===")
        Log.d(TAG, "Android Version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        Log.d(TAG, "Has Storage Permissions: ${hasStoragePermissions(context)}")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "Has MANAGE_EXTERNAL_STORAGE: ${hasManageStoragePermission()}")
        }
        
        Log.d(TAG, "Wallet Storage Dir: ${getWalletStorageDir(context)}")
        Log.d(TAG, "Backup Storage Dir: ${getBackupStorageDir(context)}")
        Log.d(TAG, "Internal Files Dir: ${context.filesDir.absolutePath}")
        Log.d(TAG, "External Files Dir: ${context.getExternalFilesDir(null)?.absolutePath ?: "null"}")
        Log.d(TAG, "========================")
    }
}
"""
    
    permission_handler_path = java_dir / "com" / "techducat" / "apo" / "PermissionHandler.kt"
    
    try:
        # Write the permission handler file
        with open(permission_handler_path, 'w') as f:
            f.write(permission_handler_content)
        
        print_success(f"Created {permission_handler_path}")
        print_info("Features included:")
        print_info("  - Android 6-13+ permission handling")
        print_info("  - SELinux-safe directory selection (fixes log warnings)")
        print_info("  - MANAGE_EXTERNAL_STORAGE support")
        print_info("  - SD card backup path support (matches WalletSuite.java)")
        print_info("  - Permission rationale handling")
        print_info("  - Comprehensive logging for debugging")
        
        # Create MainActivity integration instructions
        integration_instructions = """# Integration Instructions for PermissionHandler

Add the following to MoneroWalletActivity.kt:

## 1. Add at the top of onCreate():
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // Install splash screen (Android 12+)
    val splashScreen = installSplashScreen()
    
    super.onCreate(savedInstanceState)
    
    // CHECK PERMISSIONS FIRST
    if (!PermissionHandler.hasStoragePermissions(this)) {
        Log.w("MoneroWallet", "Storage permissions not granted, requesting...")
        PermissionHandler.requestStoragePermissions(this)
    }
    
    // Log permission status for debugging SELinux issues
    PermissionHandler.logPermissionStatus(this)
    
    // Keep splash screen visible while wallet initializes
    var keepSplashOnScreen = true
    splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
    
    walletSuite = WalletSuite.getInstance(this)
    
    // ... rest of onCreate code
}
```

## 2. Add permission result handler:
```kotlin
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    
    PermissionHandler.onRequestPermissionsResult(
        requestCode,
        permissions,
        grantResults,
        onGranted = {
            Log.i("MoneroWallet", "Storage permissions granted - wallet can proceed")
            // Permissions granted - wallet initialization can proceed normally
        },
        onDenied = {
            Log.w("MoneroWallet", "Storage permissions denied - showing rationale")
            // Show dialog explaining why permissions are needed
            showPermissionDeniedDialog()
        }
    )
}

private fun showPermissionDeniedDialog() {
    androidx.appcompat.app.AlertDialog.Builder(this)
        .setTitle("Storage Permission Required")
        .setMessage("This app needs storage permission to save your Monero wallet securely. Without this permission, the wallet cannot function properly.")
        .setPositiveButton("Grant Permission") { _, _ ->
            PermissionHandler.requestStoragePermissions(this)
        }
        .setNegativeButton("Exit") { _, _ ->
            finish()
        }
        .setCancelable(false)
        .show()
}
```

## 3. Update WalletSuite.java to use proper directories:

In `WalletSuite.java` line ~1031 (initializeWallet method), replace:
```java
// OLD CODE (causes SELinux denials):
File dir = context.getDir("wallets", Context.MODE_PRIVATE);
```

With:
```java
// NEW CODE (SELinux-safe):
// Use PermissionHandler to get proper storage directory
String storagePath = PermissionHandler.INSTANCE.getWalletStorageDir(context);
File dir = new File(storagePath, "wallets");
```

This ensures we use app-specific directories that don't trigger SELinux denials like:
```
avc: denied { write } for name="deleted" dev="vda11" ino=10874 
scontext=u:r:untrusted_app:s0:c168,c256,c512,c768 
tcontext=u:object_r:shell_data_file:s0 tclass=dir permissive=1
```

## 4. Testing

Test on multiple Android versions:
- Android 6-8: Legacy storage permissions
- Android 9-10: Scoped storage preview
- Android 11+: Scoped storage enforced
- Android 13+: Granular media permissions

Expected log output:
```
I/PermissionHandler: === PERMISSION STATUS ===
I/PermissionHandler: Android Version: 30 (11)
I/PermissionHandler: Has Storage Permissions: true
I/PermissionHandler: Has MANAGE_EXTERNAL_STORAGE: false
I/PermissionHandler: Wallet Storage Dir: /storage/emulated/0/Android/data/com.techducat.apo/files
I/PermissionHandler: Backup Storage Dir: /storage/emulated/0/Android/data/com.techducat.apo/files/backups
I/PermissionHandler: ========================
```
"""
        
        instructions_path = java_dir / "com" / "techducat" / "apo" / "PERMISSION_INTEGRATION.md"
        with open(instructions_path, 'w') as f:
            f.write(integration_instructions)
        
        print_success(f"Created {instructions_path}")
        
        return True
        
    except Exception as e:
        print_error(f"Failed to create PermissionHandler: {e}")
        return False

# Task 6: Update ProGuard rules
def update_proguard_rules(proguard_file):
    """Update proguard-rules.pro with Monero JNI rules"""
    print_header("TASK 6: Updating ProGuard Rules")
    
    # Rules to add
    new_rules = """
# ============================================================================
# MONERO WALLET SPECIFIC RULES (Added by setup_wallet.py)
# ============================================================================

# Keep all native methods for Monero JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================================
# WALLETSUITE & CORE CLASSES
# ============================================================================

# Keep WalletSuite and all nested classes/interfaces
-keep class com.techducat.apo.WalletSuite { *; }
-keep class com.techducat.apo.WalletSuite$* { *; }
-keepclassmembers class com.techducat.apo.WalletSuite {
    public *;
    protected *;
}

# Keep PermissionHandler
-keep class com.techducat.apo.PermissionHandler { *; }
-keep class com.techducat.apo.PermissionHandler$* { *; }

# ============================================================================
# MONERUJO LIBRARY (com.m2049r.xmrwallet)
# ============================================================================

# Keep ALL Monerujo classes (critical for JNI stability)
-keep class com.m2049r.xmrwallet.** { *; }
-keepclassmembers class com.m2049r.xmrwallet.** {
    public *;
    protected *;
    native <methods>;
}

# Keep specific model classes that are frequently accessed via JNI
-keep class com.m2049r.xmrwallet.model.Wallet { *; }
-keep class com.m2049r.xmrwallet.model.Wallet$* { *; }
-keep class com.m2049r.xmrwallet.model.WalletManager { *; }
-keep class com.m2049r.xmrwallet.model.WalletListener { *; }
-keep class com.m2049r.xmrwallet.model.PendingTransaction { *; }
-keep class com.m2049r.xmrwallet.model.PendingTransaction$* { *; }
-keep class com.m2049r.xmrwallet.model.TransactionInfo { *; }
-keep class com.m2049r.xmrwallet.model.TransactionInfo$* { *; }
-keep class com.m2049r.xmrwallet.model.TransactionHistory { *; }
-keep class com.m2049r.xmrwallet.model.NetworkType { *; }

# Keep Node class for daemon configuration
-keep class com.m2049r.xmrwallet.data.Node { *; }
-keep class com.m2049r.xmrwallet.data.TxData { *; }

# ============================================================================
# ENUMS USED IN JNI
# ============================================================================

# Keep all enum methods used by JNI
-keepclassmembers enum com.m2049r.xmrwallet.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# Specific enums that MUST be preserved
-keep enum com.m2049r.xmrwallet.model.Wallet$Status { *; }
-keep enum com.m2049r.xmrwallet.model.Wallet$ConnectionStatus { *; }
-keep enum com.m2049r.xmrwallet.model.Wallet$Device { *; }
-keep enum com.m2049r.xmrwallet.model.PendingTransaction$Status { *; }
-keep enum com.m2049r.xmrwallet.model.PendingTransaction$Priority { *; }
-keep enum com.m2049r.xmrwallet.model.NetworkType { *; }

# ============================================================================
# CALLBACK INTERFACES
# ============================================================================

# Keep all callback interfaces for async operations
-keep interface com.techducat.apo.WalletSuite$* { *; }
-keep class * implements com.techducat.apo.WalletSuite$* { *; }

# Keep Wallet's RescanCallback interface
-keep interface com.m2049r.xmrwallet.model.Wallet$RescanCallback { *; }
-keep class * implements com.m2049r.xmrwallet.model.Wallet$RescanCallback { *; }

# ============================================================================
# JNI STRING & REFLECTION OPERATIONS
# ============================================================================

# Keep String operations used in JNI
-keepclassmembers class java.lang.String {
    public byte[] getBytes(java.lang.String);
    public java.lang.String(byte[], java.lang.String);
}

# Keep fields accessed via reflection
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ============================================================================
# SERIALIZATION SUPPORT
# ============================================================================

# Keep serialization support for wallet data
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================================
# ATOMIC OPERATIONS
# ============================================================================

# Keep atomic classes used in WalletSuite for thread-safe operations
-keep class java.util.concurrent.atomic.** { *; }

# ============================================================================
# CHANGENOW EXCHANGE SERVICE
# ============================================================================

# Keep ChangeNowSwapService if exchange feature is enabled
-keep class com.techducat.apo.ChangeNowSwapService { *; }
-keep class com.techducat.apo.ChangeNowSwapService$* { *; }

# ============================================================================
# DEBUGGING (Remove in production)
# ============================================================================

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# Keep parameter names for debugging
-keepattributes MethodParameters

# ============================================================================
# END MONERO WALLET RULES
# ============================================================================
"""

    try:
        # Read existing ProGuard rules
        with open(proguard_file, 'r') as f:
            existing_rules = f.read()
        
        # Check if Monero rules already exist
        if "MONERO WALLET SPECIFIC RULES" in existing_rules:
            print_warning("Monero-specific rules already exist in proguard-rules.pro")
            print_info("Skipping to avoid duplicates")
            return True
        
        # Append new rules
        with open(proguard_file, 'a') as f:
            f.write("\n")
            f.write(new_rules)
        
        print_success(f"Updated {proguard_file}")
        print_info("Added rules for:")
        print_info("  - Native method preservation (JNI)")
        print_info("  - WalletSuite & PermissionHandler protection")
        print_info("  - Complete Monerujo library preservation")
        print_info("  - Enum handling (Status, ConnectionStatus, etc.)")
        print_info("  - Callback interfaces (RescanCallback, etc.)")
        print_info("  - String operations used in JNI")
        print_info("  - Atomic operations for thread safety")
        print_info("  - ChangeNOW exchange service")
        print_info("  - Debug info retention (line numbers, parameter names)")
        
        return True
        
    except FileNotFoundError:
        print_error(f"ProGuard file not found: {proguard_file}")
        return False
    except Exception as e:
        print_error(f"Failed to update ProGuard rules: {e}")
        return False

def main():
    """Main execution function"""
    print_header("MONERO WALLET SETUP SCRIPT")
    print_info("This script will:")
    print_info("  4. Add storage permissions handling (fixes SELinux denials)")
    print_info("  5. Create default wallet.properties (stagenet.xmr-tw.org)")
    print_info("  6. Update ProGuard rules for Monero JNI")
    print()
    
    # Detect project root
    current_dir = Path.cwd()
    
    # Try to find app directory
    app_dir = current_dir / "app"
    if not app_dir.exists():
        print_error("Cannot find 'app' directory")
        print_info("Please run this script from the project root (~/git/apo)")
        sys.exit(1)
    
    # Define paths
    assets_dir = app_dir / "src" / "main" / "assets"
    java_dir = app_dir / "src" / "main" / "java"
    proguard_file = app_dir / "proguard-rules.pro"
    
    print_info(f"Project root: {current_dir}")
    print_info(f"Assets dir: {assets_dir}")
    print_info(f"Java dir: {java_dir}")
    print_info(f"ProGuard file: {proguard_file}")
    print()
    
    # Verify paths exist
    if not java_dir.exists():
        print_error(f"Java directory not found: {java_dir}")
        sys.exit(1)
    
    if not proguard_file.exists():
        print_error(f"ProGuard file not found: {proguard_file}")
        sys.exit(1)
    
    # Track success
    tasks_completed = []
    tasks_failed = []
    
    # Task 5: Create wallet.properties
    if create_wallet_properties(assets_dir):
        tasks_completed.append("Task 5: wallet.properties created")
    else:
        tasks_failed.append("Task 5: wallet.properties creation failed")
    
    # Task 4: Create PermissionHandler
    if create_permission_handler(java_dir):
        tasks_completed.append("Task 4: PermissionHandler.kt created")
    else:
        tasks_failed.append("Task 4: PermissionHandler.kt creation failed")
    
    # Task 6: Update ProGuard rules
    if update_proguard_rules(proguard_file):
        tasks_completed.append("Task 6: ProGuard rules updated")
    else:
        tasks_failed.append("Task 6: ProGuard rules update failed")
    
    # Summary
    print_header("SETUP SUMMARY")
    
    if tasks_completed:
        print_success("Completed tasks:")
        for task in tasks_completed:
            print(f"  ✓ {task}")
    
    if tasks_failed:
        print()
        print_error("Failed tasks:")
        for task in tasks_failed:
            print(f"  ✗ {task}")
    
    print()
    print_header("NEXT STEPS")
    print_info("1. Review the generated files:")
    print(f"     - {assets_dir / 'wallet.properties'}")
    print(f"     - {java_dir / 'com/techducat/apo/PermissionHandler.kt'}")
    print(f"     - {proguard_file}")
    print()
    print_info("2. Integrate PermissionHandler into MoneroWalletActivity:")
    print(f"     See: {java_dir / 'com/techducat/apo/PERMISSION_INTEGRATION.md'}")
    print()
    print_info("3. Update WalletSuite.java line ~1031:")
    print("     Replace: File dir = context.getDir(\"wallets\", Context.MODE_PRIVATE);")
    print("     With: String storagePath = PermissionHandler.INSTANCE.getWalletStorageDir(context);")
    print("           File dir = new File(storagePath, \"wallets\");")
    print()
    print_info("4. Test the app:")
    print("     - Storage permissions on Android 11+ (fixes SELinux denials)")
    print("     - Wallet initialization with stagenet.xmr-tw.org")
    print("     - ProGuard build (./gradlew assembleRelease)")
    print()
    print_info("5. Verify SELinux issues are resolved:")
    print("     Check logcat for absence of:")
    print("     'avc: denied { write } ... tcontext=u:object_r:shell_data_file:s0'")
    print()
    
    # Exit code
    sys.exit(0 if not tasks_failed else 1)

if __name__ == "__main__":
    main()
