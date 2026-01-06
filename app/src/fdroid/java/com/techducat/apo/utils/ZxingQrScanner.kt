package com.techducat.apo.utils

import android.content.Context
import android.content.Intent
import com.google.zxing.integration.android.IntentIntegrator

/**
 * ZXing-based QR scanner for F-Droid builds
 */
class ZxingQrScanner : QrScannerInterface {
    override fun createScanIntent(context: Context): Intent {
        val integrator = IntentIntegrator(null)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan QR code")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(false)
        integrator.setBarcodeImageEnabled(false)
        integrator.setOrientationLocked(false)
        
        return integrator.createScanIntent()
    }
}
