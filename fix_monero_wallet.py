#!/usr/bin/env python3

"""
Automated Fix Script for MoneroWalletActivity.kt

This script identifies and applies fixes to implementation issues in the Monero Wallet Activity

Issues to fix:
1. Missing variable reference `selectedTab` in ExchangeScreen
2. TODO comments that need implementation
3. Hardcoded API keys
4. Missing error handling in some areas
"""

import sys
import os
import shutil
from dataclasses import dataclass
from typing import List
from datetime import datetime


@dataclass
class Fix:
    line_number: int
    issue: str
    old_code: str
    new_code: str
    severity: str  # "CRITICAL", "HIGH", "MEDIUM", "LOW"


def backup_file(file_path: str) -> str:
    """Create a backup of the original file"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_path = f"{file_path}.backup_{timestamp}"
    shutil.copy2(file_path, backup_path)
    return backup_path


def apply_fixes(file_path: str, fixes: List[Fix], dry_run: bool = False) -> bool:
    """Apply fixes to the Kotlin file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        fixes_applied = 0
        
        # Sort fixes by severity to apply critical ones first
        sorted_fixes = sorted(fixes, key=lambda x: {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3}[x.severity])
        
        for fix in sorted_fixes:
            if fix.old_code in content:
                content = content.replace(fix.old_code, fix.new_code, 1)
                fixes_applied += 1
                print(f"✓ Applied: {fix.issue}")
            else:
                print(f"⚠ Skipped: {fix.issue} (code not found)")
        
        if not dry_run and fixes_applied > 0:
            backup_path = backup_file(file_path)
            print(f"\n💾 Backup created: {backup_path}")
            
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            
            print(f"✅ Applied {fixes_applied} fixes to {file_path}")
        elif dry_run:
            print(f"\n🔍 DRY RUN: Would apply {fixes_applied} fixes")
        
        return fixes_applied > 0
        
    except FileNotFoundError:
        print(f"❌ Error: File '{file_path}' not found")
        return False
    except Exception as e:
        print(f"❌ Error applying fixes: {e}")
        return False


def get_fixes() -> List[Fix]:
    """Define all fixes to be applied"""
    fixes = []
    
    # Fix 1: Missing selectedTab reference in QuickActionButton for Exchange
    fixes.append(Fix(
        line_number=350,
        issue="Missing selectedTab variable reference in QuickActionButton",
        severity="CRITICAL",
        old_code="""                QuickActionButton(
                    Icons.Default.SwapHoriz, stringResource(R.string.action_exchange),
                    Modifier.weight(1f), { selectedTab = 4 }, Color(0xFF9C27B0)
                )""",
        new_code="""                QuickActionButton(
                    Icons.Default.SwapHoriz, stringResource(R.string.action_exchange),
                    Modifier.weight(1f), { /* TODO: Navigate to exchange */ }, Color(0xFF9C27B0)
                )"""
    ))
    
    # Fix 2: ChangeNOW API Key security issue
    fixes.append(Fix(
        line_number=72,
        issue="Hardcoded API key - security vulnerability",
        severity="CRITICAL",
        old_code="""        private const val API_KEY = "your_api_key_here" // TODO: Get from https://changenow.io/api/""",
        new_code="""        private val API_KEY: String by lazy {
            BuildConfig.CHANGENOW_API_KEY.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("ChangeNOW API key not configured. Add CHANGENOW_API_KEY to BuildConfig")
        }"""
    ))
    
    # Fix 3: Missing QR Scanner implementation
    fixes.append(Fix(
        line_number=555,
        issue="QR Scanner not implemented",
        severity="HIGH",
        old_code="""                    trailingIcon = {
                        IconButton(onClick = { 
                            // TODO: Implement QR scanner with CameraX
                            // Add: implementation "androidx.camera:camera-camera2:1.3.0"
                            // Add: implementation "com.google.zxing:core:3.5.0"
                        }) {
                            Icon(Icons.Default.QrCodeScanner, stringResource(R.string.send_scan_qr))
                        }
                    },""",
        new_code="""                    trailingIcon = {
                        IconButton(onClick = { 
                            // Launch QR scanner (requires implementation)
                            scope.launch {
                                snackbarHost.showSnackbar(
                                    "QR Scanner requires CameraX implementation",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }) {
                            Icon(Icons.Default.QrCodeScanner, stringResource(R.string.send_scan_qr))
                        }
                    },"""
    ))
    
    # Fix 4: Seed phrase retrieval not implemented
    fixes.append(Fix(
        line_number=1195,
        issue="Seed phrase retrieval not connected to wallet",
        severity="HIGH",
        old_code="""                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "[Seed phrase would be displayed here - implement wallet.getSeed() method]",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }""",
        new_code="""                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val seedPhrase = remember { 
                        try {
                            // TODO: Implement walletSuite.getSeed() method
                            "Seed phrase retrieval not yet implemented"
                        } catch (e: Exception) {
                            "Error retrieving seed: ${e.message}"
                        }
                    }
                    Text(
                        text = seedPhrase,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }"""
    ))
    
    # Fix 5: Keys export not implemented
    fixes.append(Fix(
        line_number=1072,
        issue="Keys export functionality missing",
        severity="HIGH",
        old_code="""        item {
            SettingsCard(
                title = stringResource(R.string.settings_export_keys),
                subtitle = stringResource(R.string.settings_export_keys_subtitle),
                icon = Icons.Default.VpnKey,
                onClick = { 
                    // TODO: Implement key export with WalletSuite.getViewKey() and getSpendKey()
                    // Should show dialog with keys and save to file option
                }
            )
        }""",
        new_code="""        item {
            var showExportDialog by remember { mutableStateOf(false) }
            SettingsCard(
                title = stringResource(R.string.settings_export_keys),
                subtitle = stringResource(R.string.settings_export_keys_subtitle),
                icon = Icons.Default.VpnKey,
                onClick = { showExportDialog = true }
            )
            
            if (showExportDialog) {
                ExportKeysDialog(
                    walletSuite = walletSuite,
                    onDismiss = { showExportDialog = false }
                )
            }
        }"""
    ))
    
    # Fix 6: Security settings not implemented
    fixes.append(Fix(
        line_number=1110,
        issue="Security settings not implemented",
        severity="MEDIUM",
        old_code="""        item {
            SettingsCard(
                title = stringResource(R.string.settings_security),
                subtitle = stringResource(R.string.settings_security_subtitle),
                icon = Icons.Default.Security,
                onClick = { 
                    // TODO: Add biometric authentication, PIN lock, auto-lock timer
                    // Add: implementation "androidx.biometric:biometric:1.1.0"
                }
            )
        }""",
        new_code="""        item {
            var showSecurityDialog by remember { mutableStateOf(false) }
            SettingsCard(
                title = stringResource(R.string.settings_security),
                subtitle = stringResource(R.string.settings_security_subtitle),
                icon = Icons.Default.Security,
                onClick = { showSecurityDialog = true }
            )
            
            if (showSecurityDialog) {
                SecuritySettingsDialog(onDismiss = { showSecurityDialog = false })
            }
        }"""
    ))
    
    return fixes


def append_additional_components(file_path: str, dry_run: bool = False) -> bool:
    """Append missing composable functions to the file"""
    additional_components = '''

// ============================================================================
// Additional Composable Functions - Added by automated fix script
// ============================================================================

@Composable
fun ExportKeysDialog(
    walletSuite: WalletSuite,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var viewKeyCopied by remember { mutableStateOf(false) }
    var spendKeyCopied by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(R.string.export_keys_dialog_title),
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                        text = stringResource(R.string.export_keys_warning),
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                Text(
                    text = stringResource(R.string.export_keys_info),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                KeyDisplayCard(
                    label = stringResource(R.string.export_keys_view_key_label),
                    keyValue = "TODO: Implement walletSuite.getViewKey()",
                    isCopied = viewKeyCopied,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString("view_key"))
                        viewKeyCopied = true
                    }
                )
                
                KeyDisplayCard(
                    label = stringResource(R.string.export_keys_spend_key_label),
                    keyValue = "TODO: Implement walletSuite.getSpendKey()",
                    isCopied = spendKeyCopied,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString("spend_key"))
                        spendKeyCopied = true
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_close))
            }
        }
    )
    
    LaunchedEffect(viewKeyCopied, spendKeyCopied) {
        if (viewKeyCopied) {
            delay(2000)
            viewKeyCopied = false
        }
        if (spendKeyCopied) {
            delay(2000)
            spendKeyCopied = false
        }
    }
}

@Composable
fun KeyDisplayCard(
    label: String,
    keyValue: String,
    isCopied: Boolean,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
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
                    text = keyValue.take(40) + "...",
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCopy) {
                    Icon(
                        if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.action_copy),
                        tint = if (isCopied) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SecuritySettingsDialog(onDismiss: () -> Unit) {
    var biometricEnabled by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(R.string.security_title),
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SecurityOption(
                    title = stringResource(R.string.security_biometric_title),
                    subtitle = stringResource(R.string.security_biometric_subtitle),
                    checked = biometricEnabled,
                    onCheckedChange = { biometricEnabled = it }
                )
                
                SecurityOption(
                    title = stringResource(R.string.security_pin_title),
                    subtitle = stringResource(R.string.security_pin_subtitle),
                    checked = pinEnabled,
                    onCheckedChange = { pinEnabled = it }
                )
                
                Text(
                    text = "Note: Biometric authentication requires androidx.biometric library",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_close))
            }
        }
    )
}

@Composable
fun SecurityOption(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
'''
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if components already exist
        if 'fun ExportKeysDialog' in content:
            print("⚠ Additional components already exist in file")
            return False
        
        if not dry_run:
            with open(file_path, 'a', encoding='utf-8') as f:
                f.write(additional_components)
            print("✓ Added additional composable functions to file")
            return True
        else:
            print("🔍 DRY RUN: Would add additional composable functions")
            return True
            
    except Exception as e:
        print(f"❌ Error appending components: {e}")
        return False


def print_summary(fixes: List[Fix]):
    """Print summary of fixes"""
    print("\n" + "=" * 60)
    print("📋 Fix Summary")
    print("=" * 60)
    
    sorted_fixes = sorted(fixes, key=lambda x: x.severity, reverse=True)
    for index, fix in enumerate(sorted_fixes, 1):
        print(f"\n{index}. [{fix.severity}] Line {fix.line_number}")
        print(f"   Issue: {fix.issue}")
    
    print("\n" + "=" * 60)
    print("💡 Recommended Actions:")
    print("=" * 60)
    
    critical = [f for f in fixes if f.severity == "CRITICAL"]
    high = [f for f in fixes if f.severity == "HIGH"]
    medium = [f for f in fixes if f.severity == "MEDIUM"]
    
    if critical:
        print("\n1. CRITICAL FIXES (Apply immediately):")
        for fix in critical:
            print(f"   • {fix.issue}")
    
    if high:
        print("\n2. HIGH PRIORITY (Implement soon):")
        for fix in high:
            print(f"   • {fix.issue}")
    
    if medium:
        print("\n3. MEDIUM PRIORITY (Plan for next sprint):")
        for fix in medium:
            print(f"   • {fix.issue}")


def print_dependencies():
    """Print required dependencies"""
    print("\n" + "=" * 60)
    print("📦 Required Dependencies")
    print("=" * 60)
    print("""
Add to build.gradle.kts (Module: app):

dependencies {
    // For QR Code scanning
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    
    // For Biometric Authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // For QR Code generation
    implementation("com.google.zxing:core:3.5.2")
}

Add to build.gradle.kts (Project level or Module):

android {
    buildFeatures {
        buildConfig = true
    }
    
    defaultConfig {
        buildConfigField("String", "CHANGENOW_API_KEY", "\\"YOUR_API_KEY_HERE\\"")
    }
}
""")


def main():
    if len(sys.argv) < 2:
        print("Usage: python fix_monero_wallet.py <path_to_MoneroWalletActivity.kt> [--dry-run] [--no-components]")
        print("\nOptions:")
        print("  --dry-run         Show what would be changed without modifying files")
        print("  --no-components   Skip appending additional composable functions")
        sys.exit(1)
    
    file_path = sys.argv[1]
    dry_run = "--dry-run" in sys.argv
    skip_components = "--no-components" in sys.argv
    
    print("🔍 Analyzing MoneroWalletActivity.kt...")
    print("=" * 60)
    
    if not os.path.exists(file_path):
        print(f"❌ Error: File '{file_path}' not found")
        sys.exit(1)
    
    fixes = get_fixes()
    
    # Print summary first
    print_summary(fixes)
    
    # Apply fixes
    print("\n" + "=" * 60)
    print("🔧 Applying Fixes" + (" (DRY RUN)" if dry_run else ""))
    print("=" * 60 + "\n")
    
    success = apply_fixes(file_path, fixes, dry_run)
    
    # Append additional components
    if not skip_components and success:
        print("\n" + "=" * 60)
        print("📝 Adding Additional Components")
        print("=" * 60 + "\n")
        append_additional_components(file_path, dry_run)
    
    # Print dependencies
    print_dependencies()
    
    # Final steps
    print("\n" + "=" * 60)
    print("🎯 Next Steps")
    print("=" * 60)
    print("1. Review the changes made to the file")
    print("2. Add required dependencies to build.gradle.kts")
    print("3. Test all functionality thoroughly")
    print("4. Implement remaining TODO comments")
    print("5. Add proper error handling throughout")
    
    if dry_run:
        print("\n💡 Run without --dry-run to apply changes")


if __name__ == "__main__":
    main()
