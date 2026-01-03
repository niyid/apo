package com.techducat.apo.ui.components
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
import java.util.UUID
import com.techducat.apo.R

// ============================================================================
// RECIPIENTCARD
// ============================================================================

// Define the RecipientInput data class if it doesn't exist
data class RecipientInput(
    val id: String = UUID.randomUUID().toString(),
    var address: String = "",
    var amount: String = ""
)

@Composable
fun RecipientCard(
    recipient: RecipientInput,
    index: Int,
    onAddressChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit,
    onPaste: () -> Unit,
    onSelectFromAddressBook: () -> Unit,
    onUseMax: () -> Unit
) {
    var addressError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recipient ${index + 1}", 
                    fontWeight = FontWeight.SemiBold
                )
                if (index > 0) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            stringResource(R.string.remove),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.address), 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onPaste,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, stringResource(R.string.paste))
                        }
                        IconButton(
                            onClick = onSelectFromAddressBook,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContactPage, stringResource(R.string.select_from_address_book))
                        }
                    }
                }
                OutlinedTextField(
                    value = recipient.address,
                    onValueChange = { newValue ->
                        onAddressChange(newValue)
                        addressError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = addressError != null,
                    supportingText = { 
                        addressError?.let { errorText ->
                            Text(errorText) 
                        }
                    },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.send_amount), 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(onClick = onUseMax) {
                        Text("MAX")
                    }
                }
                OutlinedTextField(
                    value = recipient.amount,
                    onValueChange = { newValue ->
                        onAmountChange(newValue)
                        amountError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = amountError != null,
                    supportingText = { 
                        amountError?.let { errorText ->
                            Text(errorText)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Text(stringResource(R.string.monero_symbol) + " ") }
                )
            }
        }
    }
}
