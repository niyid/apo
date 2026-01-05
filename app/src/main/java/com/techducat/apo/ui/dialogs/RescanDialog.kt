package com.techducat.apo.ui.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.techducat.apo.R

// ============================================================================
// RESCANDIALOG
// ============================================================================

@Composable
fun RescanDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                stringResource(R.string.settings_rescan_blockchain),
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Text(text = stringResource(R.string.settings_this_will_rescan_the)) 
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.rescan_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}
