package com.techducat.apo.ui.dialogs
import com.techducat.apo.ui.theme.MoneroWalletTheme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.techducat.apo.utils.generateQRCode
import com.techducat.apo.R

// ============================================================================
// PAYMENTREQUESTDIALOG
// ============================================================================

@Composable
fun PaymentRequestDialog(
    uri: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    val qrBitmap = remember(uri) {
        generateQRCode(uri, 300)
    }
    
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }
    
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.payment_request), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        modifier = Modifier.size(250.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.payment_request_qr_desc),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .background(Color.White, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        uri,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onCopy()
                        copied = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (copied) stringResource(R.string.receive_address_copied) else stringResource(R.string.action_copy))
                }
                
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(8.dp))
                    stringResource(R.string.share)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                stringResource(R.string.dialog_close)
            }
        }
    )
}
