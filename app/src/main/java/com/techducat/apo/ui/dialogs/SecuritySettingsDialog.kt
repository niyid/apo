package com.techducat.apo.ui.dialogs

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.techducat.apo.R

// ============================================================================
// SECURITYSETTINGSDIALOG
// ============================================================================

@Composable
fun SecuritySettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var biometricEnabled by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    // Get string resource for logging
    val securitySettingsTag = stringResource(R.string.settings_security)
    
    LaunchedEffect(Unit) {
        try {
            val prefs = context.getSharedPreferences("wallet_security", Context.MODE_PRIVATE)
            biometricEnabled = prefs.getBoolean("biometric_enabled", false)
            pinEnabled = prefs.getBoolean("pin_enabled", false)
        } catch (e: Exception) {
            Timber.d("$securitySettingsTag: Default settings not found, using defaults")
        }
        isLoading = false
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                stringResource(R.string.security_title),
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else {
                    SecurityOption(
                        title = "Biometric Authentication",
                        subtitle = "Use fingerprint or face recognition to access wallet",
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it },
                        enabled = !isSaving
                    )
                    
                    SecurityOption(
                        title = "PIN Protection",
                        subtitle = "Require PIN code for sensitive operations",
                        checked = pinEnabled,
                        onCheckedChange = { pinEnabled = it },
                        enabled = !isSaving
                    )
                    
                    Text(
                        text = "Note: Full implementation requires additional security modules",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            val prefs = context.getSharedPreferences("wallet_security", Context.MODE_PRIVATE)
                            prefs.edit().apply {
                                putBoolean("biometric_enabled", biometricEnabled)
                                putBoolean("pin_enabled", pinEnabled)
                                apply()
                            }
                            
                            delay(300)
                            onDismiss()
                        } catch (e: Exception) {
                            Timber.e("$securitySettingsTag: Error saving security settings", e)
                            isSaving = false
                        }
                    }
                },
                enabled = !isLoading && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.save))
            }
        }
    )
}

@Composable
fun SecurityOption(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface 
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
