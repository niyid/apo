#!/usr/bin/env python3
"""
Script to update QR code implementation for F-Droid/Play Store compatibility
Automatically updates all QR-related files to use the factory pattern.
"""

import os
import sys
import re
from pathlib import Path

def create_file(filepath, content):
    """Create a file with the given content"""
    path = Path(filepath)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, 'w') as f:
        f.write(content)
    print(f"✓ Created: {filepath}")

def update_file(filepath, content):
    """Update an existing file with new content"""
    path = Path(filepath)
    if not path.exists():
        print(f"✗ File not found: {filepath}")
        return False
    with open(path, 'w') as f:
        f.write(content)
    print(f"✓ Updated: {filepath}")
    return True

def create_qr_scanner_interface():
    """Create QR scanner interface in utils package"""
    content = '''package com.techducat.apo.utils

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
'''
    return ('app/src/main/java/com/techducat/apo/utils/QrScannerInterface.kt', content)

def create_zxing_scanner():
    """Create ZXing scanner for F-Droid"""
    content = '''package com.techducat.apo.utils

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
'''
    return ('app/src/fdroid/java/com/techducat/apo/utils/ZxingQrScanner.kt', content)

def create_mlkit_scanner():
    """Create ML Kit scanner for Play Store"""
    content = '''package com.techducat.apo.utils

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
'''
    return ('app/src/playstore/java/com/techducat/apo/utils/MlKitQrScanner.kt', content)

def create_qr_scanner_activity():
    """Create ML Kit QR scanner activity"""
    content = '''package com.techducat.apo.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.techducat.apo.R
import com.techducat.apo.ui.theme.ApoTheme
import com.techducat.apo.utils.QrScannerInterface
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * ML Kit-based QR scanner activity for Play Store builds
 */
class QrScannerActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private var hasScanned = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                getString(R.string.camera_permission_denied),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            ApoTheme {
                QrScannerScreen(
                    onQrCodeScanned = { result ->
                        if (!hasScanned) {
                            hasScanned = true
                            setResult(RESULT_OK, intent.apply {
                                putExtra(QrScannerInterface.EXTRA_QR_RESULT, result)
                            })
                            finish()
                        }
                    },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun QrScannerScreen(
        onQrCodeScanned: (String) -> Unit,
        onClose: () -> Unit
    ) {
        val context = LocalContext.current
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.send_scan_qr)) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor) { imageProxy ->
                                    processImageProxy(imageProxy, onQrCodeScanned)
                                }
                            }
                        
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalyzer
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                Text(
                    text = stringResource(R.string.qr_scan_instructions),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImageProxy(
        imageProxy: ImageProxy,
        onQrCodeScanned: (String) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !hasScanned) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        if (barcode.valueType == Barcode.TYPE_TEXT ||
                            barcode.valueType == Barcode.TYPE_URL
                        ) {
                            barcode.rawValue?.let { value ->
                                if (!hasScanned) {
                                    hasScanned = true
                                    onQrCodeScanned(value)
                                }
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
'''
    return ('app/src/playstore/java/com/techducat/apo/ui/activities/QrScannerActivity.kt', content)

def update_send_screen():
    """Update SendScreen.kt to use QR scanner factory"""
    content = '''package com.techducat.apo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.techducat.apo.R
import com.techducat.apo.utils.QrScannerFactory
import com.techducat.apo.utils.QrScannerInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    onSendTransaction: (recipient: String, amount: String, paymentId: String?) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paymentId by remember { mutableStateOf("") }
    
    // QR Scanner launcher
    val qrScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringExtra(QrScannerInterface.EXTRA_QR_RESULT)?.let { qrResult ->
                // Parse Monero URI or plain address
                recipient = parseMoneroUri(qrResult)
                
                kotlinx.coroutines.launch(kotlinx.coroutines.Dispatchers.Main) {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.qr_scanned_successfully),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.send_title)) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recipient field with QR scanner
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text(stringResource(R.string.send_recipient)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val scanner = QrScannerFactory.getScanner()
                            val intent = scanner.createScanIntent(context)
                            qrScannerLauncher.launch(intent)
                        }
                    ) {
                        Icon(Icons.Default.QrCode, stringResource(R.string.send_scan_qr))
                    }
                },
                singleLine = true
            )
            
            // Amount field
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.send_amount)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            
            // Payment ID field (optional)
            OutlinedTextField(
                value = paymentId,
                onValueChange = { paymentId = it },
                label = { Text(stringResource(R.string.send_payment_id_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Send button
            Button(
                onClick = {
                    if (recipient.isNotEmpty() && amount.isNotEmpty()) {
                        onSendTransaction(
                            recipient,
                            amount,
                            paymentId.ifEmpty { null }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = recipient.isNotEmpty() && amount.isNotEmpty()
            ) {
                Icon(Icons.Default.Send, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.send_button))
            }
        }
    }
}

/**
 * Parse Monero URI or return plain address
 * Format: monero:<address>?tx_amount=<amount>&tx_payment_id=<id>
 */
private fun parseMoneroUri(uri: String): String {
    return if (uri.startsWith("monero:")) {
        uri.substringAfter("monero:").substringBefore("?")
    } else {
        uri
    }
}
'''
    return ('app/src/main/java/com/techducat/apo/ui/screens/SendScreen.kt', content)

def update_strings_xml():
    """Create strings.xml additions"""
    content = '''
<!-- QR Scanner strings -->
<string name="qr_scan_instructions">Align the QR code within the frame</string>
<string name="qr_scanned_successfully">QR code scanned successfully</string>
<string name="qr_scan_failed">Failed to scan QR code</string>
<string name="camera_permission_required">Camera permission is required to scan QR codes</string>
<string name="camera_permission_denied">Camera permission denied. Please enable it in settings.</string>
<string name="send_scan_qr">Scan QR Code</string>
<string name="send_recipient">Recipient Address</string>
<string name="send_amount">Amount</string>
<string name="send_payment_id_optional">Payment ID (optional)</string>
<string name="send_button">Send</string>
<string name="send_title">Send</string>
'''
    return ('strings_additions.xml', content)

def main():
    """Main execution"""
    print("\n" + "="*60)
    print("QR Code Implementation Update Script")
    print("="*60 + "\n")
    
    # Check if we're in the right directory
    if not Path('app/build.gradle.kts').exists():
        print("✗ Error: Not in the root project directory")
        print("  Please run this script from your project root (where app/ folder is)")
        sys.exit(1)
    
    print("Creating/updating QR scanner implementation files...\n")
    
    files_to_create = [
        create_qr_scanner_interface(),
        create_zxing_scanner(),
        create_mlkit_scanner(),
        create_qr_scanner_activity(),
        update_send_screen(),
        update_strings_xml()
    ]
    
    for filepath, content in files_to_create:
        create_file(filepath, content)
    
    print("\n" + "="*60)
    print("✓ QR Implementation Update Complete!")
    print("="*60 + "\n")
    
    print("Next steps:")
    print("1. Add the strings from strings_additions.xml to:")
    print("   app/src/main/res/values/strings.xml")
    print("\n2. Make sure ZXing is in dependencies:")
    print("   implementation 'com.journeyapps:zxing-android-embedded:4.3.0'")
    print("\n3. Test the F-Droid build:")
    print("   ./gradlew assembleFdroidRelease")
    print("\n4. The following files DON'T need manual updates:")
    print("   - PaymentRequestDialog.kt (only generates QR, no scanning)")
    print("   - ReceiveDialog.kt (only generates QR, no scanning)")
    print("   - SubaddressDetailsDialog.kt (only generates QR, no scanning)")
    print("\n5. Commit your changes:")
    print("   git add .")
    print("   git commit -m 'Add QR scanner for F-Droid and Play Store'")

if __name__ == "__main__":
    main()
