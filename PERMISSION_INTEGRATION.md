# Integration Instructions for PermissionHandler

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
