package com.techducat.apo

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
