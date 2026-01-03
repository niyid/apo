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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.techducat.apo.WalletSuite
import com.techducat.apo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ============================================================================
// EXPORTKEYSDIALOG
// ============================================================================

@Composable
fun ExportKeysDialog(
    walletSuite: WalletSuite,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var viewKeyCopied by remember { mutableStateOf(false) }
    var spendKeyCopied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()  // Use the correct coroutine scope
    
    val viewKey = remember { walletSuite.viewKey }
    val spendKey = remember { walletSuite.spendKey }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Export Wallet Keys",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Security Warning!",
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Text(
                    text = "Anyone with these keys can access your funds. Store them securely and never share them.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                KeyDisplayCard(
                    label = stringResource(R.string.generate_viewkey_hint),
                    keyValue = viewKey,
                    isCopied = viewKeyCopied,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(viewKey))
                        viewKeyCopied = true
                    },
                    coroutineScope = coroutineScope  // Pass the correct scope
                )
                
                KeyDisplayCard(
                    label = stringResource(R.string.generate_spendkey_hint),
                    keyValue = spendKey,
                    isCopied = spendKeyCopied,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(spendKey))
                        spendKeyCopied = true
                    },
                    coroutineScope = coroutineScope  // Pass the correct scope
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_close))
            }
        }
    )
    
    LaunchedEffect(viewKeyCopied, spendKeyCopied) {
        if (viewKeyCopied) {
            delay(2000)
            viewKeyCopied = false
        }
        if (spendKeyCopied) {
            delay(2000)
            spendKeyCopied = false
        }
    }
}

@Composable
fun KeyDisplayCard(
    label: String,
    keyValue: String,
    isCopied: Boolean,
    onCopy: () -> Unit,
    coroutineScope: CoroutineScope  // Use proper CoroutineScope type
) {
    var showCopiedFeedback by remember { mutableStateOf(false) }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
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
                    text = if (keyValue.length > 40) {
                        "${keyValue.take(20)}...${keyValue.takeLast(20)}"
                    } else {
                        keyValue
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (showCopiedFeedback) {
                    Text(
                        stringResource(R.string.receive_address_copied),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                
                IconButton(
                    onClick = {
                        onCopy()
                        showCopiedFeedback = true
                        // Use the passed coroutineScope
                        coroutineScope.launch {
                            delay(2000)
                            showCopiedFeedback = false
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (showCopiedFeedback || isCopied) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.ContentCopy
                        },
                        contentDescription = "Copy key",
                        tint = if (showCopiedFeedback || isCopied) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Full key reveal option for long keys
            if (keyValue.length > 40) {
                var showFullKey by remember { mutableStateOf(false) }
                
                TextButton(
                    onClick = { showFullKey = !showFullKey },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (showFullKey) "Show Less" else "Show Full Key",
                        fontSize = 12.sp
                    )
                }
                
                if (showFullKey) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = keyValue,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp),
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
