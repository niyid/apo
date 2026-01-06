#!/bin/bash

echo "=== Complete Fix Script ==="

# 1. Create flavor-specific ApoApp
echo "1. Creating flavor-specific ApoApp files..."
./create-flavor-specific-apoapp.sh

# 2. Update SendScreen.kt
echo ""
echo "2. Updating SendScreen.kt..."
cat > app/src/main/java/com/techducat/apo/ui/screens/SendScreen.kt << 'EOF'
package com.techducat.apo.ui.screens

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
import com.techducat.apo.WalletSuite
import com.techducat.apo.utils.QrScannerFactory
import com.techducat.apo.utils.QrScannerInterface
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    walletSuite: WalletSuite,
    unlockedBalance: Long,
    onSendTransaction: (String, String) -> Unit,
    sendSuccess: String?,
    sendError: String?,
    onDismissMessages: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paymentId by remember { mutableStateOf("") }
    
    // Show success/error messages
    LaunchedEffect(sendSuccess, sendError) {
        if (sendSuccess != null) {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.transaction_sent_successfully, sendSuccess),
                duration = SnackbarDuration.Short
            )
            onDismissMessages()
        }
        if (sendError != null) {
            snackbarHostState.showSnackbar(
                message = sendError,
                duration = SnackbarDuration.Long
            )
            onDismissMessages()
        }
    }
    
    // QR Scanner launcher
    val qrScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringExtra(QrScannerInterface.EXTRA_QR_RESULT)?.let { qrResult ->
                // Parse Monero URI or plain address
                recipient = parseMoneroUri(qrResult)
                
                scope.launch {
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
            // Balance display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.available),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "\${WalletSuite.convertAtomicToXmr(unlockedBalance)} XMR",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            
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
                trailingIcon = {
                    TextButton(
                        onClick = {
                            amount = WalletSuite.convertAtomicToXmr(unlockedBalance)
                        }
                    ) {
                        Text(stringResource(R.string.max))
                    }
                },
                singleLine = true
            )
            
            // Payment ID field (optional)
            OutlinedTextField(
                value = paymentId,
                onValueChange = { paymentId = it },
                label = { Text(stringResource(R.string.send_payment_id)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Send button
            Button(
                onClick = {
                    if (recipient.isNotEmpty() && amount.isNotEmpty()) {
                        onSendTransaction(recipient, amount)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = recipient.isNotEmpty() && amount.isNotEmpty()
            ) {
                Icon(Icons.Default.Send, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.send_title))
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
EOF

echo "✓ SendScreen.kt updated"

echo ""
echo "=== Fix Complete ==="
echo "Now try: ./gradlew assembleFdroidDebug"
