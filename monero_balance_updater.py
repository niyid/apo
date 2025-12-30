#!/usr/bin/env python3
"""
Monero Wallet Balance Display Updater

This script updates the MoneroWalletActivity.kt file to:
1. Display amounts in 12 decimal places
2. Show both unlocked and locked balances separately
3. Add USD rate that updates every minute
4. Automatically updates strings.xml with required resources
"""

import re
import sys
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path
from datetime import datetime


def update_balance_display(content: str) -> str:
    """Update balance display to show 12 decimal places"""
    
    # Update convertAtomicToXmr function to use 12 decimals
    pattern = r'(fun convertAtomicToXmr\(atomic: Long\): String\s*{\s*return\s*)"%.6f"'
    replacement = r'\1"%.12f"'
    content = re.sub(pattern, replacement, content)
    
    # If the function doesn't exist, we need to add formatting where it's used
    # Update balance display format strings
    content = re.sub(
        r'"\$balanceXMR XMR"',
        r'String.format("%.12f XMR", balanceXMR.toDoubleOrNull() ?: 0.0)',
        content
    )
    
    return content


def add_locked_balance_display(content: str) -> str:
    """Add locked balance calculation and display"""
    
    # Add locked balance state variable after unlockedBalance
    pattern = r'(var unlockedBalance by remember \{ mutableStateOf\(0L\) \})'
    replacement = r'''\1
    var lockedBalance by remember { mutableStateOf(0L) }'''
    content = re.sub(pattern, replacement, content, count=1)
    
    # Update balance listener to calculate locked balance
    pattern = r'(override fun onBalanceUpdated\(bal: Long, unl: Long\) \{\s*balance = bal\s*unlockedBalance = unl)'
    replacement = r'''\1
                lockedBalance = bal - unl'''
    content = re.sub(pattern, replacement, content, count=1)
    
    # Update initial locked balance calculation
    pattern = r'(balance = walletSuite\.balanceValue\s*unlockedBalance = walletSuite\.unlockedBalanceValue)'
    replacement = r'''\1
                    lockedBalance = balance - unlockedBalance'''
    content = re.sub(pattern, replacement, content, count=1)
    
    return content


def add_usd_rate_feature(content: str) -> str:
    """Add USD rate display that updates every minute"""
    
    # Add after unlockedBalance state variable
    pattern = r'(var unlockedBalance by remember \{ mutableStateOf\(0L\) \})'
    replacement = r'''\1
    var usdRate by remember { mutableStateOf<Double?>(null) }
    var isLoadingRate by remember { mutableStateOf(false) }
    var rateError by remember { mutableStateOf<String?>(null) }'''
    content = re.sub(pattern, replacement, content, count=1)
    
    # Add USD rate fetching function before MoneroWalletScreen
    usd_fetch_function = '''
// ============================================================================
// USD RATE FETCHER
// ============================================================================

suspend fun fetchXMRUSDRate(): Result<Double> = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val url = URL("https://api.coingecko.com/api/v3/simple/price?ids=monero&vs_currencies=usd")
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("Accept", "application/json")
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val rate = json.getJSONObject("monero").getDouble("usd")
            Result.success(rate)
        } else {
            Result.failure(Exception("HTTP error: $responseCode"))
        }
    } catch (e: Exception) {
        Timber.e("USDRate", "Failed to fetch XMR/USD rate", e)
        Result.failure(e)
    } finally {
        connection?.disconnect()
    }
}

'''
    
    # Insert before MoneroWalletScreen function
    pattern = r'(@OptIn\(ExperimentalMaterial3Api::class\)\s*@Composable\s*fun MoneroWalletScreen)'
    replacement = usd_fetch_function + r'\1'
    content = re.sub(pattern, replacement, content, count=1)
    
    # Add LaunchedEffect for periodic USD rate updates
    pattern = r'(LaunchedEffect\(Unit\) \{\s*walletSuite\.setWalletStatusListener)'
    usd_update_effect = '''
    // Fetch USD rate every minute
    LaunchedEffect(Unit) {
        while (true) {
            isLoadingRate = true
            fetchXMRUSDRate()
                .onSuccess { rate ->
                    usdRate = rate
                    rateError = null
                    isLoadingRate = false
                }
                .onFailure { error ->
                    rateError = error.message
                    isLoadingRate = false
                }
            delay(60000) // Update every 60 seconds
        }
    }
    
    \1'''
    content = re.sub(pattern, usd_update_effect, content, count=1)
    
    return content


def update_home_screen_balance_card(content: str) -> str:
    """Update HomeScreen to display unlocked, locked balances and USD rate"""
    
    # Update HomeScreen function signature to include lockedBalance and usdRate
    pattern = r'fun HomeScreen\(\s*balance: Long,\s*unlockedBalance: Long,'
    replacement = '''fun HomeScreen(
    balance: Long,
    unlockedBalance: Long,
    lockedBalance: Long,
    usdRate: Double?,'''
    content = re.sub(pattern, replacement, content, count=1)
    
    # Update HomeScreen call to pass lockedBalance and usdRate
    pattern = r'0 -> HomeScreen\(\s*balance, unlockedBalance, walletAddress,'
    replacement = '''0 -> HomeScreen(
                    balance, unlockedBalance, lockedBalance, usdRate,
                    walletAddress,'''
    content = re.sub(pattern, replacement, content, count=1)
    
    # Update balance display in Card to show 12 decimals and locked balance
    pattern = r'Text\(\s*"\$balanceXMR XMR",\s*color = Color\.White,\s*fontSize = 36\.sp,\s*fontWeight = FontWeight\.Bold\s*\)\s*Text\(\s*stringResource\(R\.string\.wallet_unlocked_balance, unlockedXMR\),'
    
    replacement = '''Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    String.format("%.12f XMR", balanceXMR.toDoubleOrNull() ?: 0.0),
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                usdRate?.let { rate ->
                                    val usdValue = (balanceXMR.toDoubleOrNull() ?: 0.0) * rate
                                    Text(
                                        String.format("≈ $%.2f USD", usdValue),
                                        color = Color.White.copy(0.9f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            val lockedXMR = WalletSuite.convertAtomicToXmr(lockedBalance)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        stringResource(R.string.wallet_unlocked),
                                        color = Color.White.copy(0.7f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        String.format("%.12f", unlockedXMR.toDoubleOrNull() ?: 0.0),
                                        color = Color(0xFF4CAF50),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        stringResource(R.string.wallet_locked),
                                        color = Color.White.copy(0.7f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        String.format("%.12f", lockedXMR.toDoubleOrNull() ?: 0.0),
                                        color = Color(0xFFFF9800),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            
                            Text('''
    
    content = re.sub(pattern, replacement, content, count=1)
    
    return content


def update_strings_xml(strings_path: Path) -> bool:
    """Update strings.xml with required string resources"""
    
    try:
        # Parse the XML file
        tree = ET.parse(strings_path)
        root = tree.getroot()
        
        # Define new strings to add
        new_strings = {
            'wallet_unlocked': 'Unlocked',
            'wallet_locked': 'Locked',
            'wallet_usd_rate': 'XMR/USD Rate',
            'rate_loading': 'Loading rate…',
            'rate_unavailable': 'Rate unavailable'
        }
        
        # Get existing string names
        existing_names = {elem.get('name') for elem in root.findall('string')}
        
        # Add new strings that don't exist
        added = []
        for name, value in new_strings.items():
            if name not in existing_names:
                string_elem = ET.SubElement(root, 'string', name=name)
                string_elem.text = value
                added.append(name)
        
        if added:
            # Format the XML nicely
            indent_xml(root)
            
            # Write back to file
            tree.write(strings_path, encoding='utf-8', xml_declaration=True)
            print(f"   ✓ Added {len(added)} new string resources:")
            for name in added:
                print(f"     - {name}")
            return True
        else:
            print("   ℹ All required string resources already exist")
            return False
            
    except Exception as e:
        print(f"   ⚠ Warning: Could not update strings.xml: {e}")
        print("   You'll need to add the string resources manually")
        return False


def indent_xml(elem, level=0):
    """Add pretty-printing indentation to XML"""
    indent = "\n" + "    " * level
    if len(elem):
        if not elem.text or not elem.text.strip():
            elem.text = indent + "    "
        if not elem.tail or not elem.tail.strip():
            elem.tail = indent
        for child in elem:
            indent_xml(child, level + 1)
        if not child.tail or not child.tail.strip():
            child.tail = indent
    else:
        if level and (not elem.tail or not elem.tail.strip()):
            elem.tail = indent


def backup_file(file_path: Path) -> Path:
    """Create a timestamped backup of the file"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_path = file_path.parent / f"{file_path.stem}.backup_{timestamp}{file_path.suffix}"
    shutil.copy2(file_path, backup_path)
    return backup_path


def get_user_confirmation(prompt: str) -> bool:
    """Get yes/no confirmation from user"""
    while True:
        response = input(f"{prompt} (y/n): ").lower().strip()
        if response in ['y', 'yes']:
            return True
        elif response in ['n', 'no']:
            return False
        else:
            print("Please enter 'y' or 'n'")


def main():
    """Main function to process the Kotlin file"""
    
    # Define file paths
    kotlin_file = Path("app/src/main/java/com/techducat/apo/MoneroWalletActivity.kt")
    strings_file = Path("app/src/main/res/values/strings.xml")
    
    # Check if files exist
    if not kotlin_file.exists():
        print(f"❌ Error: {kotlin_file} not found!")
        print("Please run this script from the project root directory (~/git/apo)")
        sys.exit(1)
    
    if not strings_file.exists():
        print(f"⚠ Warning: {strings_file} not found!")
        print("String resources will need to be added manually.")
        strings_file = None
    
    print("="*60)
    print("MONERO WALLET BALANCE UPDATER")
    print("="*60)
    print(f"\n📄 Kotlin file: {kotlin_file}")
    if strings_file:
        print(f"📄 Strings file: {strings_file}")
    
    # Read and process Kotlin file
    print(f"\n📖 Reading {kotlin_file.name}...")
    content = kotlin_file.read_text(encoding='utf-8')
    
    print("🔧 Applying updates...")
    
    # Apply all transformations
    original_content = content
    content = update_balance_display(content)
    content = add_locked_balance_display(content)
    content = add_usd_rate_feature(content)
    content = update_home_screen_balance_card(content)
    
    # Check if changes were made
    if content == original_content:
        print("\n⚠ No changes detected. File may already be updated.")
        return
    
    # Create temporary output file
    temp_file = kotlin_file.parent / f"{kotlin_file.stem}_updated{kotlin_file.suffix}"
    temp_file.write_text(content, encoding='utf-8')
    
    print("\n✅ Changes applied successfully!")
    print("\n📋 Changes summary:")
    print("  1. ✓ Balance amounts now display in 12 decimal places")
    print("  2. ✓ Added locked balance display alongside unlocked balance")
    print("  3. ✓ Added USD rate that updates every minute")
    print("  4. ✓ Added USD value display for total balance")
    
    print(f"\n📝 Updated file created: {temp_file}")
    
    # Show diff information
    print("\n" + "="*60)
    print("REVIEW CHANGES")
    print("="*60)
    print("\nYou can review changes with:")
    print(f"  diff {kotlin_file} {temp_file}")
    print("\nOr with a visual diff tool:")
    print(f"  meld {kotlin_file} {temp_file}")
    print("  or")
    print(f"  code --diff {kotlin_file} {temp_file}")
    
    # Ask for confirmation
    print("\n" + "="*60)
    if not get_user_confirmation("\n⚠ Do you want to backup and replace the original file?"):
        print("\n❌ Operation cancelled.")
        print(f"   Updated file saved as: {temp_file}")
        print("   You can manually review and apply changes later.")
        return
    
    # Create backup
    print("\n📦 Creating backup...")
    backup_path = backup_file(kotlin_file)
    print(f"   ✓ Backup created: {backup_path}")
    
    # Replace original file
    print("\n🔄 Replacing original file...")
    shutil.move(str(temp_file), str(kotlin_file))
    print(f"   ✓ Original file updated: {kotlin_file}")
    
    # Update strings.xml if available
    if strings_file:
        print("\n📝 Updating strings.xml...")
        strings_backup = backup_file(strings_file)
        print(f"   ✓ Backup created: {strings_backup}")
        update_strings_xml(strings_file)
    
    # Final instructions
    print("\n" + "="*60)
    print("✅ UPDATE COMPLETE!")
    print("="*60)
    
    if not strings_file:
        print("\n⚠ MANUAL STEP REQUIRED:")
        print("  Add these string resources to app/src/main/res/values/strings.xml:")
        print("  <string name=\"wallet_unlocked\">Unlocked</string>")
        print("  <string name=\"wallet_locked\">Locked</string>")
        print("  <string name=\"wallet_usd_rate\">XMR/USD Rate</string>")
        print("  <string name=\"rate_loading\">Loading rate…</string>")
        print("  <string name=\"rate_unavailable\">Rate unavailable</string>")
    
    print("\n📋 Next steps:")
    print("  1. Build and test your application:")
    print("     ./gradlew clean assembleDebug")
    print("\n  2. Test the new features:")
    print("     - Verify 12 decimal places display")
    print("     - Check unlocked/locked balance separation")
    print("     - Confirm USD rate updates (wait ~60 seconds)")
    print("\n  3. If you need to restore the original:")
    print(f"     cp {backup_path} {kotlin_file}")
    if strings_file:
        print(f"     cp {strings_backup} {strings_file}")
    print("\n" + "="*60)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n❌ Operation cancelled by user.")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
