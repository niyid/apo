package com.techducat.apo.ui.dialogs
import com.techducat.apo.ui.theme.MoneroWalletTheme

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.techducat.apo.WalletSuite
import com.techducat.apo.R

// ============================================================================
// TRANSACTIONSEARCHDIALOG
// ============================================================================

@Composable
fun TransactionSearchDialog(walletSuite: WalletSuite, onDismiss: () -> Unit) {
    var txId by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Get string resources outside the callbacks
    val notFoundMessage = stringResource(R.string.tx_search_not_found)
    val moneroSymbol = stringResource(R.string.monero_symbol)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                stringResource(R.string.action_search_transaction), 
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.tx_search_info), 
                    fontSize = 12.sp
                )
                
                OutlinedTextField(
                    value = txId,
                    onValueChange = { 
                        txId = it
                        searchResult = null
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.tx_search_id)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSearching,
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                
                searchResult?.let {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50).copy(0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle, 
                                null, 
                                tint = Color(0xFF4CAF50), 
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                it, 
                                color = Color(0xFF4CAF50), 
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                
                errorMessage?.let {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error.copy(0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Error, 
                                null, 
                                tint = MaterialTheme.colorScheme.error, 
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                it, 
                                color = MaterialTheme.colorScheme.error, 
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSearching = true
                    errorMessage = null
                    searchResult = null
                    
                    walletSuite.searchAndImportTransaction(txId, object : WalletSuite.TransactionSearchCallback {
                        override fun onTransactionFound(txId: String, amount: Long, confirmations: Long, blockHeight: Long) {
                            isSearching = false
                            val amountXmr = WalletSuite.convertAtomicToXmr(amount)
                            searchResult = "Transaction found!\nAmount: $amountXmr $moneroSymbol\nConfirmations: $confirmations"
                        }
                        
                        override fun onTransactionNotFound(txId: String) {
                            isSearching = false
                            errorMessage = notFoundMessage
                        }
                        
                        override fun onError(error: String) {
                            isSearching = false
                            errorMessage = "Error: $error"
                        }
                    })
                },
                enabled = txId.isNotEmpty() && !isSearching
            ) {
                Text(stringResource(R.string.action_search))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_close))
            }
        }
    )
}
