package com.techducat.apo.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.techducat.apo.WalletSuite
import com.techducat.apo.storage.WalletDataStore
import com.techducat.apo.R
import com.techducat.apo.models.RecipientInput
import com.techducat.apo.models.TransactionPriority
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.text.input.KeyboardType

// ============================================================================
// ENHANCEDSENDSCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSendScreen(
    walletSuite: WalletSuite,
    dataStore: WalletDataStore,
    unlockedBalance: Long,
    onBack: () -> Unit = {},
    onSendComplete: (String) -> Unit = {}
) {
    var recipients by remember { mutableStateOf(listOf(RecipientInput(address = "", amount = ""))) }
    var selectedPriority by remember { mutableStateOf(TransactionPriority.MEDIUM) }
    var showAddressBook by remember { mutableStateOf(false) }
    var showPriorityInfo by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    var activeRecipientIndex by remember { mutableStateOf(0) }
    var totalAmount by remember { mutableStateOf(0.0) }
    var estimatedFee by remember { mutableStateOf(0.0) }
    var isSending by remember { mutableStateOf(false) }
    val unlockedXMR = WalletSuite.convertAtomicToXmr(unlockedBalance).toDoubleOrNull() ?: 0.0
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    
    // Extract string resources outside of coroutine
    val fillAllRecipientsMsg = stringResource(R.string.fill_all_recipients)
    val insufficientBalanceMsg = stringResource(R.string.insufficient_balance_transaction)
    
    // Calculate total amount and fee whenever recipients or priority changes
    LaunchedEffect(recipients, selectedPriority) {
        totalAmount = recipients.sumOf { recipient ->
            recipient.amount.toDoubleOrNull() ?: 0.0
        }
        estimatedFee = totalAmount * 0.002 * selectedPriority.feeMultiplier
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh UI state when returning to this screen
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.send_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.menu_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.available), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        Text(
                            String.format("%.6f %s", unlockedXMR, stringResource(R.string.monero_symbol)), 
                            fontSize = 24.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(stringResource(R.string.total), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        Text(
                            String.format("%.6f %s", totalAmount, stringResource(R.string.monero_symbol)), 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${stringResource(R.string.fee)}: ${String.format("%.6f %s", estimatedFee, stringResource(R.string.monero_symbol))}", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                        )
                    }
                }
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.transaction_priority), fontWeight = FontWeight.SemiBold)
                        IconButton(
                            onClick = { showPriorityInfo = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = stringResource(R.string.priority_info))
                        }
                    }
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(TransactionPriority.entries.toTypedArray()) { priority ->
                            FilterChip(
                                selected = selectedPriority == priority,
                                onClick = { selectedPriority = priority },
                                label = { 
                                    Text(
                                        when (priority) {
                                            TransactionPriority.LOW -> stringResource(R.string.slow_cheaper)
                                            TransactionPriority.MEDIUM -> stringResource(R.string.normal)
                                            TransactionPriority.HIGH -> stringResource(R.string.fast_standard)
                                            TransactionPriority.URGENT -> stringResource(R.string.urgent_fastest)
                                        }
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    enabled = true,
                                    selected = selectedPriority == priority
                                )
                            )
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recipients.size) { index ->
                    RecipientCard(
                        recipient = recipients[index],
                        index = index,
                        onAddressChange = { newAddress ->
                            recipients = recipients.mapIndexed { i, recipient ->
                                if (i == index) recipient.copy(address = newAddress) else recipient
                            }
                        },
                        onAmountChange = { newAmount ->
                            recipients = recipients.mapIndexed { i, recipient ->
                                if (i == index) recipient.copy(amount = newAmount) else recipient
                            }
                        },
                        onRemove = {
                            if (recipients.size > 1) {
                                recipients = recipients.filterIndexed { i, _ -> i != index }
                            }
                        },
                        onPaste = {
                            clipboardManager.getText()?.text?.let { text ->
                                recipients = recipients.mapIndexed { i, recipient ->
                                    if (i == index) recipient.copy(address = text) else recipient
                                }
                            }
                        },
                        onSelectFromAddressBook = {
                            activeRecipientIndex = index
                            showAddressBook = true
                        },
                        onUseMax = {
                            recipients = recipients.mapIndexed { i, recipient ->
                                if (i == index) recipient.copy(amount = unlockedXMR.toString()) else recipient
                            }
                        }
                    )
                }
                
                // Add button to add another recipient
                item {
                    Button(
                        onClick = {
                            recipients = recipients + RecipientInput(address = "", amount = "")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_another_recipient))
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val isValid = recipients.all { recipient ->
                            recipient.address.isNotEmpty() && 
                            recipient.amount.isNotEmpty() && 
                            recipient.amount.toDoubleOrNull() != null &&
                            recipient.amount.toDouble() > 0
                        }
                        
                        if (!isValid) {
                            scope.launch {
                                snackbarHost.showSnackbar(fillAllRecipientsMsg)
                            }
                            return@Button
                        }
                        
                        if (totalAmount + estimatedFee > unlockedXMR) {
                            scope.launch {
                                snackbarHost.showSnackbar(insufficientBalanceMsg)
                            }
                            return@Button
                        }
                        
                        showConfirmation = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isSending && recipients.any { it.address.isNotEmpty() && it.amount.isNotEmpty() }
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.processing))
                    } else {
                        Icon(Icons.Default.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.review_transaction))
                    }
                }
            }
        }
    }
}

@Composable
fun RecipientCard(
    recipient: RecipientInput,
    index: Int,
    onAddressChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit,
    onPaste: () -> Unit,
    onSelectFromAddressBook: () -> Unit,
    onUseMax: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Changed from stringResource(R.string.recipient_format) to simple string concatenation
                Text("${stringResource(R.string.recipient)} ${index + 1}")
                
                if (index > 0) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove))
                    }
                }
            }
            
            OutlinedTextField(
                value = recipient.address,
                onValueChange = onAddressChange,
                label = { Text(stringResource(R.string.send_recipient_address)) },
                placeholder = { Text(stringResource(R.string.hint_enter_address)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = onPaste) {
                            Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.paste))
                        }
                        IconButton(onClick = onSelectFromAddressBook) {
                            Icon(Icons.Default.Contacts, contentDescription = stringResource(R.string.select_from_address_book))
                        }
                    }
                }
            )
            
            OutlinedTextField(
                value = recipient.amount,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.send_amount)) },
                placeholder = { Text(stringResource(R.string.hint_enter_amount)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    Button(onClick = onUseMax) {
                        Text(stringResource(R.string.max))
                    }
                }
            )
        }
    }
}
