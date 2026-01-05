package com.techducat.apo.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.techducat.apo.models.RecipientInput
import com.techducat.apo.models.TransactionPriority
import com.techducat.apo.R

// ============================================================================
// TRANSACTIONCONFIRMATIONDIALOG (Multi-recipient)
// ============================================================================

@Composable
fun TransactionConfirmationDialog(
    recipients: List<RecipientInput>,
    totalAmount: Double,
    estimatedFee: Double,
    priority: TransactionPriority,
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Get string resources
    val moneroSymbol = stringResource(R.string.monero_symbol)
    val confirmSendText = stringResource(R.string.confirm_send)
    val normalText = stringResource(R.string.normal)
    
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { 
            Text(
                stringResource(R.string.send_confirm_title), 
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.total_amount), 
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                String.format("%.6f %s", totalAmount, moneroSymbol), 
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.network_fee), 
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                String.format("%.6f %s", estimatedFee, moneroSymbol), 
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.total_to_send), 
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                String.format("%.6f %s", totalAmount + estimatedFee, moneroSymbol), 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.priority), 
                        fontWeight = FontWeight.Medium
                    )
                    AssistChip(
                        onClick = {},
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ),
                        label = { 
                            Text(
                                when (priority) {
                                    TransactionPriority.LOW -> stringResource(R.string.transaction_confirmation_slow_cheaper)
                                    TransactionPriority.MEDIUM -> normalText
                                    TransactionPriority.HIGH -> stringResource(R.string.transaction_confirmation_fast_standard)
                                    TransactionPriority.URGENT -> stringResource(R.string.transaction_confirmation_urgent_fastest)
                                }
                            ) 
                        }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recipients: ${recipients.size}", 
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800))
                    Text(
                        stringResource(R.string.transaction_confirmation_monero_transactions_are_irreversible),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.processing))
                } else {
                    Icon(Icons.Default.Send, null)
                    Spacer(Modifier.width(8.dp))
                    Text(confirmSendText)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}
