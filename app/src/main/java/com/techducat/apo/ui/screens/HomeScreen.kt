package com.techducat.apo.ui.screens
import com.techducat.apo.ui.theme.MoneroWalletTheme

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.techducat.apo.WalletSuite
import com.techducat.apo.ui.components.QuickActionButton
import com.techducat.apo.R

// ============================================================================
// HOMESCREEN
// ============================================================================

@Composable
fun HomeScreen(
    balance: Long,
    unlockedBalance: Long,
    lockedBalance: Long,
    usdRate: Double?,
    walletAddress: String,
    syncProgress: Double,
    isSyncing: Boolean,
    walletHeight: Long,
    daemonHeight: Long,
    onRefresh: () -> Unit,
    onReceiveClick: () -> Unit,
    onSendClick: () -> Unit,
    onExchangeClick: () -> Unit = {}
) {
    val balanceXMR = WalletSuite.convertAtomicToXmr(balance).toDoubleOrNull() ?: 0.0
    val unlockedXMR = WalletSuite.convertAtomicToXmr(unlockedBalance).toDoubleOrNull() ?: 0.0
    val lockedXMR = WalletSuite.convertAtomicToXmr(lockedBalance).toDoubleOrNull() ?: 0.0
    val clipboardManager = LocalClipboardManager.current
    var addressCopied by remember { mutableStateOf(false) }
    
    LaunchedEffect(addressCopied) {
        if (addressCopied) {
            delay(2000)
            addressCopied = false
        }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFF6600), Color(0xFFFF8833), Color(0xFFFFAA66))
                        )
                    ).padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.wallet_total_balance),
                                color = Color.White.copy(0.9f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            val infiniteTransition = rememberInfiniteTransition(label = "refresh")
                            val rotation by infiniteTransition.animateFloat(
                                0f, if (isSyncing) 360f else 0f,
                                infiniteRepeatable(tween(1000, easing = LinearEasing)),
                                label = "rotation"
                            )
                            
                            IconButton(onClick = onRefresh) {
                                Icon(
                                    Icons.Default.Refresh, stringResource(R.string.action_refresh),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp).rotate(if (isSyncing) rotation else 0f)
                                )
                            }
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                String.format("%.6f %s", balanceXMR, stringResource(R.string.monero_symbol)),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            usdRate?.let { rate ->
                                val usdValue = balanceXMR * rate
                                Text(
                                    String.format("≈ $%.2f USD", usdValue),
                                    color = Color.White.copy(0.9f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    stringResource(R.string.wallet_unlocked),
                                    color = Color.White.copy(0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    String.format("%.6f %s", unlockedXMR, stringResource(R.string.monero_symbol)),
                                    color = Color(0xFF1B5E20),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    stringResource(R.string.wallet_locked),
                                    color = Color.White.copy(0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    String.format("%.6f %s", lockedXMR, stringResource(R.string.monero_symbol)),
                                    color = Color.White.copy(0.85f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        if (isSyncing) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LinearProgressIndicator(
                                    progress = (syncProgress / 100.0).toFloat(),
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(0.3f)
                                )
                                Text(
                                    String.format("Syncing: %.1f%% (%d/%d)", syncProgress, walletHeight, daemonHeight),
                                    color = Color.White.copy(0.9f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    Icons.Default.CallReceived, stringResource(R.string.nav_receive),
                    Modifier.weight(1f), onReceiveClick, Color(0xFF4CAF50)
                )
                QuickActionButton(
                    Icons.Default.Send, stringResource(R.string.nav_send),
                    Modifier.weight(1f), onSendClick, Color(0xFFFF6600)
                )
                QuickActionButton(
                    Icons.Default.SwapHoriz, stringResource(R.string.nav_exchange),
                    Modifier.weight(1f), onExchangeClick, Color(0xFF9C27B0)
                )
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.wallet_your_address), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (walletAddress.length > 40) 
                                    "${walletAddress.take(20)}...${walletAddress.takeLast(20)}"
                                else walletAddress,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 2
                            )
                            
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(walletAddress))
                                addressCopied = true
                            }) {
                                Icon(
                                    if (addressCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    stringResource(R.string.action_copy),
                                    tint = if (addressCopied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
