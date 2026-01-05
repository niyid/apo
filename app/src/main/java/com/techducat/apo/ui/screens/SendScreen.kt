package com.techducat.apo.ui.screens
import com.techducat.apo.ui.theme.MoneroWalletTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.techducat.apo.WalletSuite
import com.techducat.apo.R

@Composable
fun SendScreen(
    walletSuite: WalletSuite,
    unlockedBalance: Long,
    onSend: (String, Double) -> Unit,
    successMessage: String? = null,
    errorMessage: String? = null,
    onDismissMessage: () -> Unit = {}
) {
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var showConfirmation by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var showTxSearch by remember { mutableStateOf(false) }
    
    val unlockedXMR = WalletSuite.convertAtomicToXmr(unlockedBalance).toDoubleOrNull() ?: 0.0
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Extract string resources at composable level
    val qrScannerNotImplementedMsg = stringResource(R.string.send_qr_scanner_not_implemented)
    val creatingTransactionText = stringResource(R.string.creating_transaction)
    val actionSearchTransactionText = stringResource(R.string.action_search_transaction)
    
    LaunchedEffect(successMessage, errorMessage) {
        successMessage?.let {
            scope.launch {
                snackbarHost.showSnackbar(
                    "Transaction sent! TX: ${it.take(16)}...",
                    duration = SnackbarDuration.Long
                )
            }
        }
        errorMessage?.let {
            scope.launch {
                snackbarHost.showSnackbar(it, duration = SnackbarDuration.Long)
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(stringResource(R.string.send_send_xmr), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.send_available_balance), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                            Text(
                                String.format("%.6f %s", unlockedXMR, stringResource(R.string.monero_symbol)), 
                                fontSize = 28.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Icon(Icons.Default.AccountBalance, null, tint = Color(0xFF4CAF50).copy(0.3f), modifier = Modifier.size(48.dp))
                    }
                }
            }
            
            item {
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text(stringResource(R.string.send_recipient_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(onClick = { 
                            scope.launch {
                                snackbarHost.showSnackbar(
                                    qrScannerNotImplementedMsg,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }) {
                            Icon(Icons.Default.QrCode, stringResource(R.string.send_scan_qr))
                        }
                    },
                    maxLines = 3
                )
            }
            
            item {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        TextButton(onClick = { amount = unlockedXMR.toString() }) {
                            Text("MAX", fontWeight = FontWeight.Bold)
                        }
                    },
                    singleLine = true,
                    prefix = { Text(stringResource(R.string.monero_symbol) + " ") }
                )
            }
            
            item {
                Button(
                    onClick = { 
                        if (!isSending) showConfirmation = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = recipient.isNotEmpty() && amount.isNotEmpty() && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(creatingTransactionText)
                    } else {
                        Icon(Icons.Default.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.send_transaction), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            item {
                TextButton(
                    onClick = { showTxSearch = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text(actionSearchTransactionText)
                }
            }
        }
        
        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
