package com.techducat.apo.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.techducat.apo.R
import com.techducat.apo.models.AddressBookEntry

// ============================================================================
// ADDEDITADDRESSDIALOG
// ============================================================================

@Composable
fun AddEditAddressDialog(
    entry: AddressBookEntry?,
    onSave: (AddressBookEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(entry?.name ?: "") }
    var address by remember { mutableStateOf(entry?.address ?: "") }
    var notes by remember { mutableStateOf(entry?.notes ?: "") }
    var isFavorite by remember { mutableStateOf(entry?.isFavorite ?: false) }
    var addressError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (entry == null) stringResource(R.string.add_contact) else "Edit Contact",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.contact_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isEmpty(),
                    supportingText = {
                        if (name.isEmpty()) {
                            Text(stringResource(R.string.contact_name_required))
                        }
                    }
                )
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { 
                        address = it
                        addressError = null
                    },
                    label = { Text(stringResource(R.string.monero_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    isError = addressError != null,
                    supportingText = {
                        if (addressError != null) {
                            Text(addressError ?: "")
                        } else if (!MoneroUriHandler.isMoneroAddress(address) && address.isNotEmpty()) {
                            Text(stringResource(R.string.doesnt_look_like_valid_address))
                        }
                    }
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.send_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.add_to_favorites), fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isEmpty()) {
                        return@Button
                    }
                    
                    if (!MoneroUriHandler.isMoneroAddress(address)) {
                        addressError = "Invalid Monero address"
                        return@Button
                    }
                    
                    onSave(
                        entry?.copy(
                            name = name,
                            address = address,
                            notes = notes,
                            isFavorite = isFavorite
                        ) ?: AddressBookEntry(
                            name = name,
                            address = address,
                            notes = notes,
                            isFavorite = isFavorite
                        )
                    )
                },
                enabled = name.isNotEmpty() && MoneroUriHandler.isMoneroAddress(address),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (entry == null) stringResource(R.string.add_contact) else stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

// Note: MoneroUriHandler should be imported/available
object MoneroUriHandler {
    fun isMoneroAddress(address: String): Boolean {
        return address.startsWith("4") || (address.startsWith("8") && address.length >= 95 && address.length <= 106)
    }
}
