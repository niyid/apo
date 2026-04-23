package com.techducat.apo.ui.components
import com.techducat.apo.ui.theme.MoneroWalletTheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.techducat.apo.R

// ============================================================================
// TWOROWNAVIGATIONBAR
// ============================================================================

@Composable
fun TwoRowNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // First Row: Main Navigation (4 items)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = stringResource(R.string.nav_wallet),
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )
                NavItem(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = stringResource(R.string.nav_send),
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )
                NavItem(
                    icon = Icons.Default.History,
                    label = stringResource(R.string.nav_history),
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )
                NavItem(
                    icon = Icons.Default.Settings,
                    label = stringResource(R.string.nav_settings),
                    selected = selectedTab == 3,
                    onClick = { onTabSelected(3) }
                )
            }
            
                HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
            
            // Second Row: Additional Features (4 items)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavItem(
                    icon = Icons.Default.ContactPage,
                    label = stringResource(R.string.nav_contacts),
                    selected = selectedTab == 4,
                    onClick = { onTabSelected(4) }
                )
                NavItem(
                    icon = Icons.Default.LocationOn,
                    label = stringResource(R.string.nav_subaddresses),
                    selected = selectedTab == 5,
                    onClick = { onTabSelected(5) }
                )
                NavItem(
                    icon = Icons.Default.QrCode,
                    label = stringResource(R.string.nav_request),
                    selected = selectedTab == 6,
                    onClick = { onTabSelected(6) }
                )
                NavItem(
                    icon = Icons.Default.Tune,
                    label = stringResource(R.string.nav_enhanced_send_short),
                    selected = selectedTab == 7,
                    onClick = { onTabSelected(7) }
                )
            }
        }
    }
}

// Note: The string resources (R.string.*) must be defined in your strings.xml
// NavItem composable must be imported/available
