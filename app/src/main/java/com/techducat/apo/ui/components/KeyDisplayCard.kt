package com.techducat.apo.ui.components
import com.techducat.apo.ui.theme.MoneroWalletTheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.techducat.apo.R

// ============================================================================
// KEYDISPLAYCARD
// ============================================================================

@Composable
fun KeyDisplayCard(
    label: String,
    keyValue: String,
    isCopied: Boolean,
    onCopy: () -> Unit
) {
    var showCopiedFeedback by remember { mutableStateOf(false) }
    
    // Use LaunchedEffect to reset the copied feedback after 2 seconds
    LaunchedEffect(showCopiedFeedback) {
        if (showCopiedFeedback) {
            delay(2000)
            showCopiedFeedback = false
        }
    }
    
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
