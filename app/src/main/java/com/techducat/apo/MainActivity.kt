package com.techducat.apo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.techducat.apo.storage.WalletDataStore
import com.techducat.apo.ui.screens.MoneroWalletScreen
import com.techducat.apo.ui.theme.MoneroWalletTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private lateinit var walletSuite: WalletSuite
    private lateinit var dataStore: WalletDataStore

    // Register for activity result using the modern API
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Timber.i("Storage permission granted")
        } else {
            Timber.w("Storage permission denied")
            // Log which specific permissions were denied
            permissions.entries.filter { !it.value }.forEach { (permission, _) ->
                Timber.w("Permission denied: $permission")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        dataStore = WalletDataStore(this)
        
        if (!PermissionHandler.hasStoragePermissions(this)) {
            Timber.w("Storage permission not granted - requesting")
            PermissionHandler.requestStoragePermissions(this, requestPermissionLauncher)
        }
        
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        
        walletSuite = WalletSuite.getInstance(this)
            
        setContent {
            MoneroWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoneroWalletScreen(walletSuite, dataStore)
                }
            }
        }
        
        lifecycleScope.launch {
            delay(1500)
            keepSplashOnScreen = false
        }
    }
    
    override fun onDestroy() {
        if (isFinishing) {
            Timber.d("Activity finishing - closing wallet")
            try {
                walletSuite.close()
            } catch (e: Exception) {
                Timber.e("Error closing wallet", e)
            }
        }
        super.onDestroy()
    }
}
