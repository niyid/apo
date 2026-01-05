package com.techducat.apo.ui.screens

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
import com.techducat.apo.ui.components.EmptyState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.techducat.apo.WalletSuite
import com.techducat.apo.models.Transaction
import com.m2049r.xmrwallet.model.TransactionInfo
import com.techducat.apo.ui.components.TransactionCard
import com.techducat.apo.R

// ============================================================================
// HISTORYSCREEN
// ============================================================================

@Composable
fun HistoryScreen(walletSuite: WalletSuite) {
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    // Get string resources
    val receivedText = stringResource(R.string.history_received)
    val sentText = stringResource(R.string.history_sent)
    
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            walletSuite.getTransactionHistory(object : WalletSuite.TransactionHistoryCallback {
                override fun onSuccess(txList: List<TransactionInfo>) {
                    transactions = txList.map { txInfo ->
                        val isReceived = txInfo.direction == TransactionInfo.Direction.Direction_In
                        val absoluteAmount = if (txInfo.amount < 0) -txInfo.amount else txInfo.amount
                        Transaction(
                            type = if (isReceived) receivedText else sentText,
                            amount = String.format(
                                "%.6f XMR",
                                WalletSuite.convertAtomicToXmr(absoluteAmount).toDoubleOrNull() ?: 0.0
                            ),
                            date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                .format(Date(txInfo.timestamp * 1000)),
                            confirmed = !txInfo.isPending,
                            txId = txInfo.hash
                        )
                    }
                    isLoading = false
                }
                
                override fun onError(error: String) {
                    errorMessage = error
                    isLoading = false
                }
            })
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.history_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    IconButton(onClick = { 
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            delay(500)
                            isLoading = false
                        }
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            
            if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                errorMessage ?: stringResource(R.string.unknown_error),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            items(count = transactions.size) { index ->
                TransactionCard(transaction = transactions[index])
            }
            
            if (transactions.isEmpty() && !isLoading && errorMessage == null) {
                item {
                    EmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = stringResource(R.string.empty_transactions),
                        message = stringResource(R.string.history_your_transaction_history_will)
                    )
                }
            }
        }
    }
}
