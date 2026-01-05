package com.techducat.apo.ui.screens
import com.techducat.apo.ui.theme.MoneroWalletTheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.techducat.apo.WalletSuite
import com.techducat.apo.ui.components.SettingsCard
import com.techducat.apo.R
// Add these imports for the dialogs
import com.techducat.apo.ui.dialogs.RescanDialog
import com.techducat.apo.ui.dialogs.SeedPhraseDialog
import com.techducat.apo.ui.dialogs.NodeConfigDialog
import com.techducat.apo.ui.dialogs.TransactionSearchDialog
import com.techducat.apo.ui.dialogs.ExportKeysDialog
import com.techducat.apo.ui.dialogs.SecuritySettingsDialog

// ============================================================================
// SETTINGSSCREEN
// ============================================================================

@Composable
fun SettingsScreen(
    walletSuite: WalletSuite,
    walletAddress: String
) {
    var showRescanDialog by remember { mutableStateOf(false) }
    var showSeedDialog by remember { mutableStateOf(false) }
    var showNodeDialog by remember { mutableStateOf(false) }
    var showTxSearchDialog by remember { mutableStateOf(false) }
    var showExportKeysDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var rescanProgress by remember { mutableStateOf(0.0) }
    var isRescanning by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Extract string resource at composable level
    val syncStartedMsg = stringResource(R.string.settings_sync_started)

    LaunchedEffect(Unit) {
        walletSuite.setRescanBalanceCallback(object : WalletSuite.RescanBalanceCallback {
            override fun onBalanceUpdated(balance: Long, unlockedBalance: Long) {
                val status = walletSuite.syncStatus
                rescanProgress = status.percentDone
                isRescanning = status.syncing
            }
        })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.nav_settings),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.settings_wallet_info_1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.settings_status),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = if (walletSuite.isSyncing) stringResource(R.string.settings_syncing) else stringResource(R.string.settings_ready),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.settings_maintenance),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                SettingsCard(
                    title = stringResource(R.string.settings_rescan_blockchain),
                    subtitle = if (isRescanning) "Rescanning: ${String.format("%.1f%%", rescanProgress)}" else stringResource(R.string.settings_rescan_subtitle),
                    icon = Icons.Default.Refresh,
                    onClick = { showRescanDialog = true },
                    enabled = !isRescanning
                )
            }

            item {
                SettingsCard(
                    title = stringResource(R.string.settings_force_refresh),
                    subtitle = stringResource(R.string.settings_force_refresh_subtitle),
                    icon = Icons.Default.Refresh,
                    onClick = { 
                        walletSuite.triggerImmediateSync()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = syncStartedMsg,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }

            item {
                Text(
                    stringResource(R.string.settings_backup_security),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                SettingsCard(
                    title = stringResource(R.string.settings_view_seed),
                    subtitle = stringResource(R.string.settings_view_seed_subtitle),
                    icon = Icons.Default.Key,
                    onClick = { showSeedDialog = true }
                )
            }

            item {
                SettingsCard(
                    title = stringResource(R.string.settings_export_keys),
                    subtitle = stringResource(R.string.settings_export_keys_subtitle),
                    icon = Icons.Default.Key,
                    onClick = { showExportKeysDialog = true }
                )
            }

            item {
                Text(
                    stringResource(R.string.settings_network),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                SettingsCard(
                    title = stringResource(R.string.settings_node_settings),
                    subtitle = "Node: ${walletSuite.daemonAddress}:${walletSuite.daemonPort}",
                    icon = Icons.Default.Cloud,
                    onClick = { showNodeDialog = true }
                )
            }

            item {
                SettingsCard(
                    title = stringResource(R.string.settings_tx_search),
                    subtitle = stringResource(R.string.settings_tx_search_subtitle),
                    icon = Icons.Default.Search,
                    onClick = { showTxSearchDialog = true }
                )
            }

            item {
                SettingsCard(
                    title = stringResource(R.string.settings_security),
                    subtitle = stringResource(R.string.settings_security_subtitle),
                    icon = Icons.Default.Lock,
                    onClick = { showSecurityDialog = true }
                )
            }
        }
    }
    
    // Dialogs
    if (showRescanDialog) {
        RescanDialog(
            onConfirm = {
                showRescanDialog = false
                isRescanning = true
                walletSuite.rescanBlockchain()
            },
            onDismiss = { showRescanDialog = false }
        )
    }

    if (showSeedDialog) {
        SeedPhraseDialog(
            walletSuite = walletSuite,
            onDismiss = { showSeedDialog = false }
        )
    }

    if (showNodeDialog) {
        NodeConfigDialog(
            walletSuite = walletSuite,
            onDismiss = { showNodeDialog = false }
        )
    }

    if (showTxSearchDialog) {
        TransactionSearchDialog(
            walletSuite = walletSuite,
            onDismiss = { showTxSearchDialog = false }
        )
    }

    if (showExportKeysDialog) {
        ExportKeysDialog(
            walletSuite = walletSuite,
            onDismiss = { showExportKeysDialog = false }
        )
    }

    if (showSecurityDialog) {
        SecuritySettingsDialog(
            onDismiss = { showSecurityDialog = false }
        )
    }
}
