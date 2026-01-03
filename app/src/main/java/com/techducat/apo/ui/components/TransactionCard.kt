package com.techducat.apo.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.techducat.apo.models.Transaction
import com.techducat.apo.R

// ============================================================================
// TRANSACTIONCARD
// ============================================================================

@Composable
fun TransactionCard(transaction: Transaction) {
    var expanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var txIdCopied by remember { mutableStateOf(false) }
    
    // Get string resources at composable level
    val receivedText = stringResource(R.string.history_received)
    val confirmedText = stringResource(R.string.history_confirmed)
    val pendingText = stringResource(R.string.history_pending)
    val txSearchIdText = stringResource(R.string.tx_search_id)
    val actionCopyText = stringResource(R.string.action_copy)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val isReceived = transaction.type == receivedText
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isReceived) 
                                    Color(0xFF4CAF50).copy(alpha = 0.2f)
                                else 
                                    Color(0xFFFF6600).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isReceived) Icons.Default.CallReceived else Icons.Default.Send,
                            contentDescription = null,
                            tint = if (isReceived) Color(0xFF4CAF50) else Color(0xFFFF6600),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.type,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = transaction.date,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (transaction.type == receivedText) "+" else "-"}${transaction.amount}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.type == receivedText) 
                            Color(0xFF4CAF50) 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    if (transaction.confirmed) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                confirmedText,
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                pendingText,
                                fontSize = 11.sp,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        txSearchIdText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (transaction.txId.length > 40)
                                "${transaction.txId.take(20)}...${transaction.txId.takeLast(20)}"
                            else transaction.txId,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(transaction.txId))
                                txIdCopied = true
                            }
                        ) {
                            Icon(
                                if (txIdCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = actionCopyText,
                                tint = if (txIdCopied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    LaunchedEffect(txIdCopied) {
        if (txIdCopied) {
            delay(2000)
            txIdCopied = false
        }
    }
}
