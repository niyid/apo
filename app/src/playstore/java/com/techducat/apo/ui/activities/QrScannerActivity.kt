package com.techducat.apo.ui.activities

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
