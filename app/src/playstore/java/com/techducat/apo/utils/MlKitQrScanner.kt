package com.techducat.apo.utils

import android.content.Context
import android.content.Intent
import com.techducat.apo.ui.activities.QrScannerActivity

/**
 * ML Kit-based QR scanner for Play Store builds
 */
class MlKitQrScanner : QrScannerInterface {
    override fun createScanIntent(context: Context): Intent {
        return Intent(context, QrScannerActivity::class.java)
    }
}
