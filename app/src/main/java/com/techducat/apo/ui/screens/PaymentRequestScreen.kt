package com.techducat.apo.ui.screens
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.techducat.apo.models.PaymentRequest
import com.techducat.apo.utils.MoneroUriHandler
import com.techducat.apo.R

// ============================================================================
// PAYMENTREQUESTSCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRequestScreen(
    walletAddress: String,
    onBack: () -> Unit = {}
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var generatedUri by remember { mutableStateOf<String?>(null) }
    var showUriDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.payment_request), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.menu_back))
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
                        Icons.Default.QrCode,
                        contentDescription = null,
                        tint = Color(0xFF2196F3)
                    )
                    Text(
                        "Generate a payment request QR code that includes amount and description",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(stringResource(R.string.wallet_your_address), fontWeight = FontWeight.SemiBold)
                        Text(
                            walletAddress.take(20) + "..." + walletAddress.takeLast(20),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { stringResource(R.string.send_amount) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.enter_amount_optional)) },
                    prefix = { Text(stringResource(R.string.monero_symbol) + " ") },
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { stringResource(R.string.recipient_name_optional) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.recipient_name_example)) },
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { stringResource(R.string.receive_desc_hint) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    placeholder = { Text(stringResource(R.string.description_example)) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    val request = PaymentRequest(
                        address = walletAddress,
                        amount = if (amount.isNotEmpty()) amount else null,
                        recipientName = if (recipientName.isNotEmpty()) recipientName else null,
                        txDescription = if (description.isNotEmpty()) description else null
                    )
                    
                    generatedUri = MoneroUriHandler.createUri(request)
                    showUriDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                enabled = amount.isEmpty() || amount.toDoubleOrNull() != null
            ) {
                Icon(Icons.Default.QrCode, null)
                Spacer(Modifier.width(8.dp))
                stringResource(R.string.generate_payment_request)
            }
        }
    }
}
