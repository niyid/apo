package com.techducat.apo.utils

import android.content.Context
import android.content.Intent

/**
 * Interface for QR code scanning across different implementations
 * - Play Store: Uses Google ML Kit
 * - F-Droid: Uses ZXing
 */
interface QrScannerInterface {
    /**
     * Create an intent to launch the QR scanner
     */
    fun createScanIntent(context: Context): Intent
    
    companion object {
        const val EXTRA_QR_RESULT = "qr_result"
    }
}

/**
 * Factory to get the appropriate QR scanner implementation
 */
object QrScannerFactory {
    fun getScanner(): QrScannerInterface {
        return try {
            // Try to load Play Store implementation (ML Kit)
            Class.forName("com.techducat.apo.utils.MlKitQrScanner")
                .getDeclaredConstructor()
                .newInstance() as QrScannerInterface
        } catch (e: ClassNotFoundException) {
            // Fall back to F-Droid implementation (ZXing)
            ZxingQrScanner()
        }
    }
}
