package com.techducat.apo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.techducat.apo.ui.components.EmptyState
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.techducat.apo.WalletSuite
import com.techducat.apo.storage.WalletDataStore
import com.techducat.apo.ui.components.SubaddressCard
import com.techducat.apo.models.Subaddress
import com.techducat.apo.R
import androidx.compose.ui.text.AnnotatedString

// ============================================================================
// SUBADDRESSSCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubaddressScreen(
    walletSuite: WalletSuite,
    dataStore: WalletDataStore,
    onBack: () -> Unit = {}
) {
    var subaddresses by remember { mutableStateOf(dataStore.loadSubaddresses()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedSubaddress by remember { mutableStateOf<Subaddress?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    
    // Extract string resource outside of coroutine to avoid composable invocation error
    val messageCopyAddress = stringResource(R.string.message_copy_address)
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.nav_subaddresses),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.menu_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCreateDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.create_subaddress)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF2196F3)
                    )
                    Text(
                        text = "Subaddresses improve privacy by generating unique addresses that forward to your main wallet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (subaddresses.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.AddLocation,
                    title = stringResource(R.string.no_subaddresses),
                    message = "Create your first subaddress to improve privacy"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subaddresses.sortedByDescending { it.creationTime }) { sub ->
                        SubaddressCard(
                            subaddress = sub,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(sub.address))
                                scope.launch {
                                    snackbarHost.showSnackbar(messageCopyAddress)
                                }
                            },
                            onClick = {
                                selectedSubaddress = sub
                                showDetailsDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}
