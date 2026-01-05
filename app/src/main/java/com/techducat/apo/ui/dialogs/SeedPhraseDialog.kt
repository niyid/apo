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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.techducat.apo.WalletSuite
import com.techducat.apo.R

// ============================================================================
// SEEDPHRASEDIALOG
// ============================================================================

@Composable
fun SeedPhraseDialog(
    walletSuite: WalletSuite,
    onDismiss: () -> Unit
) {
    var seedPhrase by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        walletSuite.getSeedAsync(object : WalletSuite.SeedCallback {
            override fun onSuccess(seed: String) {
                seedPhrase = seed
                isLoading = false
            }
            
            override fun onError(errorMsg: String) {
                error = errorMsg
                isLoading = false
            }
        })
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                stringResource(R.string.seed_title),
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        text = stringResource(R.string.seed_phrase_security_warning),
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = stringResource(R.string.seed_phrase_anyone_with_this_seed),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val clipboardManager = LocalClipboardManager.current
                        var seedCopied by remember { mutableStateOf(false) }
                        
                        when {
                            isLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            error != null -> {
                                Text(
                                    text = error ?: stringResource(R.string.unknown_error),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                )
                            }
                            seedPhrase != null -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = seedPhrase ?: "",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            seedPhrase?.let {
                                                clipboardManager.setText(AnnotatedString(it))
                                                seedCopied = true
                                            }
                                        },
                                        enabled = seedPhrase != null
                                    ) {
                                        Icon(
                                            if (seedCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = stringResource(R.string.action_copy),
                                            tint = if (seedCopied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                        
                        LaunchedEffect(seedCopied) {
                            if (seedCopied) {
                                delay(2000)
                                seedCopied = false
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_close))
            }
        }
    )
}
