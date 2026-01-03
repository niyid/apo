package com.techducat.apo.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.techducat.apo.R

@Composable
fun CreateSubaddressDialog(
    existingLabels: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var labelError by remember { mutableStateOf<String?>(null) }
    
    // Get the error message string outside the lambda
    val labelExistsError = stringResource(R.string.subaddress_label_exists)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(R.string.create_new_subaddress), 
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        if (existingLabels.contains(it)) {
                            labelError = labelExistsError
                        } else {
                            labelError = null
                        }
                    },
                    label = { Text(stringResource(R.string.subaddress_label)) },
                    placeholder = { Text(stringResource(R.string.subaddress_label_placeholder)) },
                    isError = labelError != null,
                    supportingText = { 
                        if (labelError != null) {
                            Text(labelError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(stringResource(R.string.label_exists_choose_unique))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (label.isNotBlank() && !existingLabels.contains(label)) {
                        onConfirm(label)
                    }
                },
                enabled = label.isNotBlank() && labelError == null
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}
