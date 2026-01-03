package com.techducat.apo.ui.components
import com.techducat.apo.ui.theme.MoneroWalletTheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.techducat.apo.network.ChangeNowSwapService
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
                    icon = Icons.Default.Send,
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
            
            Divider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
            
            // Second Row: Additional Features (5 items)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavItem(
                    icon = Icons.Default.SwapHoriz,
                    label = stringResource(R.string.nav_exchange),
                    selected = selectedTab == 4,
                    onClick = { onTabSelected(4) },
                    enabled = ChangeNowSwapService.isConfigured()
                )
                NavItem(
                    icon = Icons.Default.ContactPage,
                    label = stringResource(R.string.nav_contacts),
                    selected = selectedTab == 5,
                    onClick = { onTabSelected(5) }
                )
                NavItem(
                    icon = Icons.Default.LocationOn,
                    label = stringResource(R.string.nav_subaddresses),
                    selected = selectedTab == 6,
                    onClick = { onTabSelected(6) }
                )
                NavItem(
                    icon = Icons.Default.QrCode,
                    label = stringResource(R.string.nav_request),
                    selected = selectedTab == 7,
                    onClick = { onTabSelected(7) }
                )
                NavItem(
                    icon = Icons.Default.Tune,
                    label = stringResource(R.string.nav_enhanced_send_short),
                    selected = selectedTab == 8,
                    onClick = { onTabSelected(8) }
                )
            }
        }
    }
}

// Note: Based on the original file, you'll need to make sure:
// 1. ChangeNowSwapService.isConfigured() is available (defined elsewhere)
// 2. The string resources (R.string.*) are defined in your strings.xml
// 3. NavItem composable is imported/available
